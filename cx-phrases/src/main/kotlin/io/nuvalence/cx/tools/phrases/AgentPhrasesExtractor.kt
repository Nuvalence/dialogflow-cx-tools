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
            processEventHandlers(jsonObject, flowName).forEach { (event, channelToLanguagesMap) ->
                // Iterate over each channel and pass all languages to LanguagePhrases
                channelToLanguagesMap.forEach { (channel, languagesMap) ->
                    // Filter out any languages with empty message lists
                    val nonEmptyLanguagesMap = languagesMap.filter { it.value.isNotEmpty() }

                    // If there are languages with messages, use them
                    if (nonEmptyLanguagesMap.isNotEmpty()) {
                        translationAgent.putFlow(
                            PhrasePath(listOf(flowName, "", "event", event, channel)),
                            LanguagePhrases(nonEmptyLanguagesMap)
                        )
                    }
                }
            }
            jsonObject["transitionRoutes"]?.asJsonArray?.forEach { route ->
                route.asJsonObject["condition"]?.asString?.let { condition ->
                    // If there are transition route fulfillment messages, capture them
                    processMessages(route.asJsonObject["triggerFulfillment"])?.let { channelToLanguagesMap ->
                        // Iterate over each channel and pass all languages to LanguagePhrases
                        channelToLanguagesMap.forEach { (channel, languagesMap) ->
                            // Check the condition to potentially update the channel
                            val updatedChannel = if (condition.contains("is-chatbot-interaction = true")) "DF_MESSENGER" else channel

                            // Filter out any languages with empty message lists
                            val nonEmptyLanguagesMap = languagesMap.filter { it.value.isNotEmpty() }

                            // If there are languages with messages, use them
                            if (nonEmptyLanguagesMap.isNotEmpty()) {
                                translationAgent.putFlow(
                                    PhrasePath(listOf(flowName, "", "condition", condition, updatedChannel)),
                                    LanguagePhrases(nonEmptyLanguagesMap)
                                )
                            }
                        }
                    }
                }
                // Get all transition routes and their associated condition
                route.asJsonObject["intent"]?.asString?.let { intent ->
                    // If there are trigger fulfillment messages, capture them
                    processMessages(route.asJsonObject["triggerFulfillment"])?.let { channelToLanguagesMap ->
                        // Now, iterate over each channel and pass all languages to LanguagePhrases
                        channelToLanguagesMap.forEach { (channel, languagesMap) ->
                            // Filter out any languages with empty message lists
                            val nonEmptyLanguagesMap = languagesMap.filter { it.value.isNotEmpty() }

                            // If there are languages with messages, use them
                            if (nonEmptyLanguagesMap.isNotEmpty()) {
                                translationAgent.putFlow(
                                    PhrasePath(listOf(flowName, "", "intent", intent, channel)),
                                    LanguagePhrases(nonEmptyLanguagesMap)
                                )
                            }
                        }
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
            val flowName = directory.name
            File("${directory.absolutePath}/pages").listFiles()?.forEach { file ->
                val pageName = file.name.removeSuffix(".json")
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject

                jsonObject["entryFulfillment"]?.let { processMessagesAndTranslate(it, flowName, pageName, translationAgent) }
                processTransitionRoutes(jsonObject, flowName, pageName, translationAgent)
                processForm(jsonObject, flowName, pageName, translationAgent)
                processEventHandlers(jsonObject, flowName, pageName, translationAgent)
            }
        }
    }

    private fun processMessagesAndTranslate(entryFulfillment: JsonElement, flowName: String, pageName: String, translationAgent: TranslationAgent) {
        val messages = processMessages(entryFulfillment)
        messages?.forEach { (channel, languagesMap) ->
            languagesMap.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
                translationAgent.putPage(
                    PhrasePath(listOf(flowName, pageName, "message", channel)),
                    LanguagePhrases(it)
                )
            }
        }
    }

    private fun processTransitionRoutes(jsonObject: JsonObject, flowName: String, pageName: String, translationAgent: TranslationAgent) {
        jsonObject["transitionRoutes"]?.asJsonArray?.forEach { route ->
            route.asJsonObject["condition"]?.asString?.let { condition ->
                processMessages(route.asJsonObject["triggerFulfillment"])?.let { channelToLanguagesMap ->
                    channelToLanguagesMap.forEach { (originalChannel, languagesMap) ->
                        val updatedChannel = if (condition.contains("is-chatbot-interaction = true")) "DF_MESSENGER" else originalChannel
                        languagesMap.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
                            translationAgent.putFlow(
                                PhrasePath(listOf(flowName, pageName, "condition", condition, updatedChannel)),
                                LanguagePhrases(it)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun processForm(jsonObject: JsonObject, flowName: String, pageName: String, translationAgent: TranslationAgent) {
        jsonObject["form"]?.asJsonObject?.get("parameters")?.asJsonArray?.forEach { parameterElement ->
            val parameter = parameterElement.asJsonObject
            val displayName = parameter["displayName"].asString

            processInitialPrompts(parameter, displayName, flowName, pageName, translationAgent)
            processRepromptEventHandlers(parameter, displayName, flowName, pageName, translationAgent)
        }
    }

    private fun processInitialPrompts(parameter: JsonObject, displayName: String, flowName: String, pageName: String, translationAgent: TranslationAgent) {
        parameter["fillBehavior"]?.asJsonObject?.get("initialPromptFulfillment")?.let { initialPrompt ->
            val messages = processMessages(initialPrompt)
            if (!messages.isNullOrEmpty()) {
                messages.forEach { (channel, languagesMap) ->
                    languagesMap.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
                        translationAgent.putPage(
                            PhrasePath(listOf(flowName, pageName, "$displayName\ninitialPromptFulfillment", channel)),
                            LanguagePhrases(it)
                        )
                    }
                }
            }
        }
    }

    private fun processRepromptEventHandlers(parameter: JsonObject, displayName: String, flowName: String, pageName: String, translationAgent: TranslationAgent) {
        parameter["fillBehavior"]?.asJsonObject?.get("repromptEventHandlers")?.asJsonArray?.forEach { event ->
            val messages = processMessages(event.asJsonObject["triggerFulfillment"])
            val eventName = event.asJsonObject["event"].asString
            if (!messages.isNullOrEmpty()) {
                messages.forEach { (channel, languagesMap) ->
                    languagesMap.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
                        translationAgent.putPage(
                            PhrasePath(listOf(flowName, pageName, "$displayName\nrepromptEventHandlers\n$eventName", channel)),
                            LanguagePhrases(it)
                        )
                    }
                }
            }
        }
    }

    private fun processEventHandlers(jsonObject: JsonObject, flowName: String, pageName: String, translationAgent: TranslationAgent) {
        jsonObject["eventHandlers"]?.asJsonArray?.forEach { eventHandlerElement ->
            val eventHandler = eventHandlerElement.asJsonObject
            val eventName = eventHandler["event"].asString

            processMessages(eventHandler["triggerFulfillment"])?.let { channelToLanguagesMap ->
                channelToLanguagesMap.forEach { (channel, languagesMap) ->
                    // Filter out any languages with empty message lists
                    val nonEmptyLanguagesMap = languagesMap.filter { it.value.isNotEmpty() }

                    // If there are languages with messages, use them
                    if (nonEmptyLanguagesMap.isNotEmpty()) {
                        translationAgent.putPage(
                            PhrasePath(listOf(flowName, pageName, eventName, channel)),
                            LanguagePhrases(nonEmptyLanguagesMap)
                        )
                    }
                }
            }
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
        val messagesJsonArray = jsonElement.asJsonObject["messages"]?.asJsonArray ?: return emptyMap()

        return messagesJsonArray.flatMap { element ->
            val jsonObject = element.asJsonObject
            val languageCode = jsonObject["languageCode"]?.asString ?: return@flatMap emptyList()
            val channel = jsonObject["channel"]?.asString ?: "audio"

            val messages = extractTextMessages(jsonObject) + extractPayloadMessages(jsonObject)
            if (messages.isNotEmpty()) listOf(Triple(channel, languageCode, messages)) else emptyList()
        }.groupBy({ it.first }, { Pair(it.second, it.third) })
            .mapValues { (_, pairs) ->
                pairs.groupBy({ it.first }, { it.second })
                    .mapValues { (_, messagesList) -> messagesList.flatten() }  // Ensure this returns List<String>
            }
    }

    private fun extractTextMessages(jsonObject: JsonObject): List<String> =
        jsonObject["text"]?.asJsonObject?.get("text")?.asJsonArray?.mapNotNull { it.asString } ?: emptyList()

    private fun extractPayloadMessages(jsonObject: JsonObject): List<String> =
        jsonObject["payload"]?.asJsonObject?.get("richContent")?.asJsonArray?.flatMap { innerArray ->
            innerArray.asJsonArray.mapNotNull { contentItem ->
                when (contentItem.asJsonObject["type"]?.asString) {
                    "html" -> contentItem.asJsonObject["html"].asString
                    "chips" -> contentItem.asJsonObject["options"]?.asJsonArray?.joinToString("\n") { option ->
                        option.asJsonObject["text"]?.asString ?: ""
                    }
                    else -> null
                }
            }
        } ?: emptyList()


    /**
     * Process event handlers and, if they have trigger fulfillment messages, extract them. Note that
     * we are treating the sys.no-match-default and sys.no-input-default as special cases - we capture
     * those for the Default Start Flow only, since we will reuse their fulfillment messages everywhere
     * when we import the agent.
     */
    private fun processEventHandlers(jsonElement: JsonElement, flowName: String): Map<String, Map<String, Map<String, List<String>>>> =
        jsonElement.asJsonObject["eventHandlers"]?.asJsonArray?.mapNotNull { handler ->
            val event = handler.asJsonObject["event"].asString
            val messages = processMessages(handler.asJsonObject["triggerFulfillment"])
            if (messages != null && (flowName == "Default Start Flow" ||
                        event != "sys.no-match-default" && event != "sys.no-input-default")
            ) {
                event to messages
            } else null
        }?.toMap() ?: mapOf()
}