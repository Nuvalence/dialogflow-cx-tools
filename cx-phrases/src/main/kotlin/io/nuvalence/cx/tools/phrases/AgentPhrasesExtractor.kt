package io.nuvalence.cx.tools.phrases

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * Extracts phrases from an exploded agent.zip export and capture them in
 * a TranslationAgent instance.
 *
 * @param rootPath path to where the exploded agent is.
 */
class AgentPhrasesExtractor(private val rootPath: String) {
    fun process(): TranslationAgent {
        val translationAgent = processAgent()
        processEntityTypes(translationAgent)
        processIntents(translationAgent)
        processFlows(translationAgent)
        processPages(translationAgent)
        return translationAgent
    }

    /**
     * Extract the default and supported language codes from agent.json, and use them to create
     * the TranslationAgent instance that will be used to hold the rest of the phrases.
     */
    private fun processAgent(): TranslationAgent {
        val jsonObject = JsonParser.parseString(File("$rootPath/agent.json").readText()).asJsonObject
        // Both are at the top level.
        val defaultLanguageCode = jsonObject["defaultLanguageCode"].asString
        val supportedLanguageCodes = jsonObject["supportedLanguageCodes"]?.asJsonArray?.map { it.asString } ?: listOf()
        return TranslationAgent(defaultLanguageCode, supportedLanguageCodes)
    }

    /**
     * Process all intent phrases. They are located under the "intents" directory under the
     * agent root path. Each intent has its own directory, whose name matches the intent name,
     * with a "trainingPhrases" subdirectory. Under it, there is one file per language, with the
     * language code representing the file name. For example, english training phrases for an
     * intent called "some-intent" are located under:
     *
     * <agent-root>/intents/some-intent/trainingPhrases/en.json
     */
    private fun processIntents(translationAgent: TranslationAgent) {
        // Walk all directories under the "intents" directory
        File("$rootPath/intents").listFiles()?.forEach { directory ->
            val intentPath = directory.absolutePath
            val intentName = directory.name // The directory name is the intent name
            // Walk all existing language directories capturing the messages
            val messages = File("$intentPath/trainingPhrases").listFiles()?.associate { file ->
                val language = file.nameWithoutExtension // as in es.json minus .json
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                // Training phrases are under a "trainingPhrases" JSON attribute, which is an array
                val trainingPhrases = jsonObject["trainingPhrases"].asJsonArray.map { element ->
                    processParts(element.asJsonObject["parts"]) // Training phrases have parts
                }
                language to trainingPhrases
            }
            if (messages != null)
                translationAgent.putIntent(PhrasePath(listOf(intentName)), LanguagePhrases(messages))
        }
    }

    /**
     * Extract the entity types. They are located under the entityTypes directory under the agent
     * root path. Each entity type has its own directory, whose name matches its display name. The
     * entities subdirectory contains one file per language, with the entity value ant its synonyms.
     */
    private fun processEntityTypes(translationAgent: TranslationAgent) {
        File("$rootPath/entityTypes").listFiles()?.forEach { directory ->
            val entityPath = directory.absolutePath
            val entityName = directory.name // the directory name is the entity name
            File("$entityPath/entities").listFiles()?.forEach { file ->
                val language = file.nameWithoutExtension // as in en.json minus .json
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                jsonObject["entities"]?.asJsonArray?.forEach { entity ->
                    val value = entity.asJsonObject["value"].asString
                    val synonyms = entity.asJsonObject["synonyms"].asJsonArray.toList().map { it.asString }
                    translationAgent.putEntity(entityName, value, language, synonyms)
                }
            }
        }
    }

    /**
     * Process files under <agent-root>/flows/flow-name/flow-name.json. These contain events and transitions
     * based on conditions.
     */
    private fun processFlows(translationAgent: TranslationAgent) {
        // Walk all directories under the "flows" directory
        File("$rootPath/flows").listFiles()?.forEach { directory ->
            val flowPath = directory.absolutePath
            val flowName = directory.name // The directory name is the flow name
            val jsonObject = JsonParser.parseString(File("$flowPath/$flowName.json").readText()).asJsonObject
            // Get all event handlers associated with this flow
            processEventHandlers(jsonObject, flowName).forEach { (event, messages) ->
                translationAgent.putFlow(PhrasePath(listOf(flowName, "", "event", event)), messages)
            }
            // Get all transition routes and their associated condition or intent
            jsonObject["transitionRoutes"].asJsonArray.forEach { route ->
                val messages = processMessages(route.asJsonObject["triggerFulfillment"])

                route.asJsonObject["condition"]?.asString?.let { condition ->
                    // If there are trigger fulfillment messages, capture them
                    messages.forEach { (key, value) ->
                        val adjustedKey = if (key.startsWith("chatbot-")) "chatbot-condition" else "condition"
                        translationAgent.putFlow(
                            PhrasePath(listOf(flowName, "", adjustedKey, condition)),
                            LanguagePhrases(value)
                        )
                    }
                }

                route.asJsonObject["intent"]?.asString?.let { intent ->
                    // If there are trigger fulfillment messages, capture them
                    messages.forEach { (key, value) ->
                        val adjustedKey = if (key.startsWith("chatbot-")) "chatbot-intent" else "intent"
                        translationAgent.putFlow(
                            PhrasePath(listOf(flowName, "", adjustedKey, intent)),
                            LanguagePhrases(value)
                        )
                    }
                }
            }
        }
    }

    /**
     * Process files under <agent-root>/flows/flow-name/pages. These contain the pages that
     * belong to a flow.
     */
    private fun processPages(translationAgent: TranslationAgent) {
        File("$rootPath/flows").listFiles()?.forEach { directory ->
            val flowPath = directory.absolutePath
            val flowName = directory.name // The directory name is the flow name
            File("$flowPath/pages").listFiles()?.forEach { file ->
                val pageName = file.nameWithoutExtension // as in <page-name>.json minus .json
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                processEntryFulfillment(jsonObject, translationAgent, flowName, pageName)
                processTransitionRoutes(jsonObject, translationAgent, flowName, pageName)
                processForm(jsonObject, translationAgent, flowName, pageName)
                processPageEventHandlers(jsonObject, translationAgent, flowName, pageName)
            }
        }
    }

    /**
     * Processes the entry fulfillment for a given page within a flow.
     *
     * Entry fulfillment messages are extracted from the provided JSON object which represents
     * the structure of a Dialogflow CX page. These messages are then processed and added to the
     * TranslationAgent under the specified flow and page.
     *
     * @param jsonObject The JSON object representing the Dialogflow CX page configuration, containing the entry fulfillment messages.
     * @param translationAgent The TranslationAgent instance where the phrases and messages will be stored.
     * @param flowName The name of the flow to which the page belongs. Used for organizing the phrases within the TranslationAgent.
     * @param pageName The name of the page being processed. Used alongside the flowName to organize the phrases within the TranslationAgent.
     */
    private fun processEntryFulfillment(jsonObject: JsonObject, translationAgent: TranslationAgent, flowName: String, pageName: String) {
        jsonObject["entryFulfillment"]?.let { entryFulfillment ->
            val elements = processMessages(entryFulfillment)
            if (!elements.isNullOrEmpty()) {
                elements.forEach { (messageType, phraseByLanguage) ->
                    if (phraseByLanguage.isNotEmpty()) {
                        translationAgent.putPage(
                            PhrasePath(listOf(flowName, pageName, messageType)),
                            LanguagePhrases(phraseByLanguage)
                        )
                    }
                }
            }
        }
    }

    /**
     * Processes the transition routes for a given page within a flow.
     *
     * This function iterates through the transition routes defined in a Dialogflow CX page's JSON configuration.
     * For each route, it extracts the condition and associated trigger fulfillment messages. These messages are then
     * processed and added to the TranslationAgent. If the messages are part of a chatbot element, their condition
     * is adjusted to reflect this by prepending "chatbot-" to the condition string.
     *
     * @param jsonObject The JSON object representing the Dialogflow CX page configuration, containing transition routes.
     * @param translationAgent The TranslationAgent instance where the phrases and messages will be stored.
     * @param flowName The name of the flow to which the page belongs. Used for organizing the phrases within the TranslationAgent.
     * @param pageName The name of the page being processed. Used alongside the flowName to organize the phrases within the TranslationAgent.
     */
    private fun processTransitionRoutes(jsonObject: JsonObject, translationAgent: TranslationAgent, flowName: String, pageName: String) {
        jsonObject["transitionRoutes"]?.asJsonArray?.forEach { route ->
            route.asJsonObject["condition"]?.asString?.let { condition ->
                val messages = processMessages(route.asJsonObject["triggerFulfillment"])
                messages.forEach { (messageType, phraseByLanguage) ->
                    val adjustedCondition = if (messageType.startsWith("chatbot-")) "chatbot-condition" else "condition"
                    translationAgent.putFlow(PhrasePath(listOf(flowName, pageName, adjustedCondition, condition)), LanguagePhrases(phraseByLanguage))
                }
            }
        }
    }


    /**
     * Processes the form parameters of a given page within a flow.
     *
     * This function examines the "form" section of a Dialogflow CX page's JSON configuration.
     * It iterates through each parameter defined under the form, capturing its display name and
     * processing its fill behavior. The fill behavior describes how the parameter should be
     * populated and potentially reprompted.
     *
     * @param jsonObject The JSON object representing the Dialogflow CX page configuration, containing form parameters.
     * @param translationAgent The TranslationAgent instance where the phrases and messages will be stored.
     * @param flowName The name of the flow to which the page belongs. Used for organizing the phrases within the TranslationAgent.
     * @param pageName The name of the page being processed. Used alongside the flowName to organize the phrases within the TranslationAgent.
     */
    private fun processForm(jsonObject: JsonObject, translationAgent: TranslationAgent, flowName: String, pageName: String) {
        jsonObject["form"]?.asJsonObject?.get("parameters")?.asJsonArray?.forEach { parameterElement ->
            val parameter = parameterElement.asJsonObject
            val displayName = parameter["displayName"].asString
            processFillBehavior(parameter, translationAgent, flowName, pageName, displayName)
        }
    }

    /**
     * Processes the fill behavior for a specific parameter within a form on a Dialogflow CX page.
     *
     * This function focuses on the "fillBehavior" aspect of a form's parameter, which defines how the parameter
     * should be filled and potentially reprompted for. The function delegates the detailed
     * processing of initial prompts and reprompts to dedicated methods.
     *
     * @param parameter The JSON object representing a single parameter within the form, containing the fill behavior details.
     * @param translationAgent The TranslationAgent instance where the phrases and messages will be stored.
     * @param flowName The name of the flow to which the page belongs. Used for organizing the phrases within the TranslationAgent.
     * @param pageName The name of the page being processed. Used alongside the flowName to organize the phrases within the TranslationAgent.
     * @param displayName The display name of the parameter being processed. Used for reference in the phrase paths and potential logging.
     */
    private fun processFillBehavior(parameter: JsonObject, translationAgent: TranslationAgent, flowName: String, pageName: String, displayName: String) {
        parameter["fillBehavior"]?.asJsonObject?.let { fillBehavior ->
            processInitialPromptFulfillment(fillBehavior, translationAgent, flowName, pageName, displayName)
            processRepromptEventHandlers(fillBehavior, translationAgent, flowName, pageName, displayName)
        }
    }

    /**
     * Processes the initial prompt fulfillment for a specific parameter within a form on a Dialogflow CX page.
     *
     * This function addresses the 'initialPromptFulfillment' behavior defined within the parameter's fill behavior.
     * It extracts and processes the messages intended to initially prompt the user to provide input for the parameter.
     * These messages are then appropriately categorized and stored in the TranslationAgent. If the messages are part
     * of a chatbot element, their keys are adjusted to reflect this by prepending "chatbot" to the key.
     *
     * @param fillBehavior The JSON object representing the fill behavior of a parameter, containing details about initial prompts.
     * @param translationAgent The TranslationAgent instance where the phrases and messages will be stored.
     * @param flowName The name of the flow to which the page belongs. Used for organizing the phrases within the TranslationAgent.
     * @param pageName The name of the page being processed. Used alongside the flowName to organize the phrases within the TranslationAgent.
     * @param displayName The display name of the parameter. Used as part of the key to organize and retrieve the phrases within the TranslationAgent.
     */
    private fun processInitialPromptFulfillment(fillBehavior: JsonObject, translationAgent: TranslationAgent, flowName: String, pageName: String, displayName: String) {
        fillBehavior["initialPromptFulfillment"]?.let { initialPrompt ->
            val messages = processMessages(initialPrompt)
            messages.forEach { (key, value) ->
                val adjustedKey = if (key.startsWith("chatbot-")) "chatbot\n$displayName\ninitialPromptFulfillment" else "$displayName\ninitialPromptFulfillment"
                translationAgent.putPage(PhrasePath(listOf(flowName, pageName, adjustedKey)), LanguagePhrases(value))
            }
        }
    }

    /**
     * Processes the reprompt event handlers for a specific parameter within a form on a Dialogflow CX page.
     *
     * This function examines the 'repromptEventHandlers' defined within a parameter's fill behavior in the form.
     * It iterates through each event handler, extracting and processing the messages that are triggered upon specific
     * events (like 'no input' or 'no match'). These messages are intended to reprompt the user for input. The function
     * ensures that these messages are appropriately categorized and stored in the TranslationAgent. If the messages
     * correspond to a chatbot element, their keys are adjusted to include "chatbot" for clear identification and retrieval.
     *
     *
     * @param fillBehavior The JSON object representing the fill behavior of a parameter, containing details about reprompt event handlers.
     * @param translationAgent The TranslationAgent instance where the phrases and messages will be stored.
     * @param flowName The name of the flow to which the page belongs. Used for organizing the phrases within the TranslationAgent.
     * @param pageName The name of the page being processed. Used alongside the flowName to organize the phrases within the TranslationAgent.
     * @param displayName The display name of the parameter. Used as part of the key to organize and retrieve the phrases within the TranslationAgent.
     */
    private fun processRepromptEventHandlers(fillBehavior: JsonObject, translationAgent: TranslationAgent, flowName: String, pageName: String, displayName: String) {
        fillBehavior["repromptEventHandlers"]?.asJsonArray?.forEach { event ->
            val messages = processMessages(event.asJsonObject["triggerFulfillment"])
            val eventName = event.asJsonObject["event"].asString
            messages.forEach { (key, value) ->
                val adjustedKey = if (key.startsWith("chatbot-")) "chatbot\n$displayName\nrepromptEventHandlers\n$eventName" else "$displayName\nrepromptEventHandlers\n$eventName"
                translationAgent.putPage(PhrasePath(listOf(flowName, pageName, adjustedKey)), LanguagePhrases(value))
            }
        }
    }

    /**
     * Processes the event handlers associated with a specific page within a flow.
     *
     * This function focuses on capturing the messages and phrases triggered by event handlers on a Dialogflow CX page.
     * Event handlers are used to define how a page responds to specific events, such as user input or session start.
     * This function extracts those messages and stores them in the TranslationAgent, categorizing them under the specific
     * flow and page they belong to. It utilizes the `processEventHandlers` method to extract and process the messages,
     * then organizes them within the TranslationAgent using a structured path that includes the flow and page names.
     *
     * @param jsonObject The JSON object representing the Dialogflow CX page configuration, containing event handlers.
     * @param translationAgent The TranslationAgent instance where the phrases and messages will be stored.
     * @param flowName The name of the flow to which the page belongs. Used as part of the path for organizing the phrases within the TranslationAgent.
     * @param pageName The name of the page being processed. Used as part of the path for organizing the phrases within the TranslationAgent.
     */
    private fun processPageEventHandlers(jsonObject: JsonObject, translationAgent: TranslationAgent, flowName: String, pageName: String) {
        processEventHandlers(jsonObject, flowName).forEach { (event, messages) ->
            translationAgent.putPage(PhrasePath(listOf(flowName, pageName, event)), messages)
        }
    }


    /**
     * Process the parts of a message. For intents, this also includes references to entities that
     * are to be captured as part of the intent. Messages are an array of texts that may have a
     * reference to a parameterId, and the actual message is a concatenation of all those texts.
     * When we find parameters, we capture them as [text](parameterId), loosely following the
     * README.md convention for links.
     */
    private fun processParts(jsonElement: JsonElement) =
        jsonElement.asJsonArray.joinToString("") { element ->
            val items = element.asJsonObject.asMap()
            if (items.containsKey("parameterId"))
                "[${items["text"]?.asString}](${items["parameterId"]?.asString})"
            else
                items["text"]?.asString ?: ""
        }

    /**
     * Process all messages under the supplied jsonElement. Messages have a languageCode
     * and a text field; under it, you find a list of text messages. Unlike for intents,
     * fulfillment messages do not have references to parameters.
     */
    private fun processMessages(jsonElement: JsonElement): Map<String, Map<String, List<String>>> {
        val resultMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()

        jsonElement.asJsonObject["messages"]?.asJsonArray?.forEach { element ->
            val languageCode = element.asJsonObject["languageCode"].asString

            // Process rich content elements like chips and HTML
            element.asJsonObject["payload"]?.asJsonObject?.get("richContent")?.asJsonArray?.forEach { outerList ->
                outerList.asJsonArray.forEach { customElement ->
                    val elementType = customElement.asJsonObject["type"].asString
                    val key = "chatbot-$elementType"

                    when (elementType) {
                        "chips" -> {
                            val chipsValues = customElement.asJsonObject["options"].asJsonArray.map { chip ->
                                chip.asJsonObject["text"].asString
                            }
                            resultMap.getOrPut(key) { mutableMapOf() }
                                .getOrPut(languageCode) { mutableListOf() }
                                .addAll(chipsValues)
                        }

                        "html" -> {
                            val htmlContent = customElement.asJsonObject["html"].asString
                            if (htmlContent.isNotBlank()) {
                                resultMap.getOrPut(key) { mutableMapOf() }
                                    .getOrPut(languageCode) { mutableListOf() }
                                    .add(htmlContent)
                            }
                        }
                    }
                }
            }

            // Process text elements
            val outerText = element.asJsonObject["text"]
            outerText?.asJsonObject?.get("text")?.asJsonArray?.mapNotNull { it.asString }?.let { texts ->
                if (texts.isNotEmpty()) {
                    resultMap.getOrPut("messages") { mutableMapOf() }
                        .getOrPut(languageCode) { mutableListOf() }
                        .addAll(texts)
                }
            }
        }

        // Convert mutable inner maps and lists to immutable ones
        return resultMap.mapValues { (_, value) -> value.mapValues { (_, list) -> list.toList() }.toMap() }
    }


    /**
     * Process event handlers and, if they have trigger fulfillment messages, extract them. Note that
     * we are treating the sys.no-match-default and sys.no-input-default as special cases - we capture
     * those for the Default Start Flow only, since we will reuse their fulfillment messages everywhere
     * when we import the agent.
     */
    private fun processEventHandlers(jsonElement: JsonElement, flowName: String): Map<String, LanguagePhrases> {
        val resultsMap = mutableMapOf<String, LanguagePhrases>()

        jsonElement.asJsonObject["eventHandlers"]?.asJsonArray?.forEach { handler ->
            val event = handler.asJsonObject["event"].asString
            val messages = processMessages(handler.asJsonObject["triggerFulfillment"])

            messages.forEach { (key, value) ->
                // Check if the key indicates a chatbot element and adjust the event accordingly
                val adjustedEvent = if (key.startsWith("chatbot-")) "chatbot\n$event" else event

                // Filter out the messages based on the flowName and event conditions
                if (value.isNotEmpty() && (flowName == "Default Start Flow" ||
                            event != "sys.no-match-default" && event != "sys.no-input-default")
                ) {
                    resultsMap[adjustedEvent] = LanguagePhrases(value)
                }
            }
        }

        return resultsMap
    }
}
