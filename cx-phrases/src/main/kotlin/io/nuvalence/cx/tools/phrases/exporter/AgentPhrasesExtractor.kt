package io.nuvalence.cx.tools.phrases.exporter

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.nuvalence.cx.tools.phrases.util.LanguageMessages
import io.nuvalence.cx.tools.phrases.util.Message
import io.nuvalence.cx.tools.phrases.util.PhrasePath
import io.nuvalence.cx.tools.phrases.util.TranslationAgent
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
                language to listOf(Message(trainingPhrases))
            }
            if (messages != null)
                translationAgent.putIntent(PhrasePath(listOf(intentName)), LanguageMessages(messages))
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
            val entityName = directory.name // The directory name is the entity name
            val entityKind = JsonParser.parseString(File("$entityPath/$entityName.json").readText()).asJsonObject["kind"].asString

            val languageFiles = File("$entityPath/entities").listFiles()
            val sortedLanguageFiles = languageFiles?.sortedBy {
                translationAgent.allLanguages.indexOf(it.nameWithoutExtension)
            }

            val regexList = mutableListOf<String>()
            val phraseMap = mutableMapOf<PhrasePath, MutableMap<String, List<Message>>>()
            sortedLanguageFiles?.forEach { file ->
                val language = file.nameWithoutExtension // as in en.json minus .json
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                var i = 0
                jsonObject["entities"]?.asJsonArray?.forEach { entity ->
                    if (entityKind == "KIND_REGEXP") {
                        if (language == "en") {
                            regexList.add(i, entity.asJsonObject["value"].asString)
                        }
                    }
                    val value = entity.asJsonObject["value"].asString
                    val synonyms = entity.asJsonObject["synonyms"].asJsonArray.map { it.asString }
                    val message = Message(synonyms, null, entityKind, null)
                    val phraseLangMap = if (entityKind == "KIND_REGEXP") {
                        phraseMap.getOrPut(PhrasePath(listOf(entityName, regexList[i++]))) { mutableMapOf() }
                    }
                    else
                        phraseMap.getOrPut(PhrasePath(listOf(entityName, value))) { mutableMapOf() }
                    phraseLangMap[language] = listOf(message)
                }
            }

            phraseMap.entries.forEach { (phrasePath, languageMessages) ->
                translationAgent.putEntity(phrasePath, LanguageMessages(languageMessages))
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
            // Get all transition routes and their associated condition
            jsonObject["transitionRoutes"].asJsonArray.forEach { route ->
                route.asJsonObject["condition"]?.asString?.let { condition ->
                    // If there are trigger fulfillment messages, capture them
                    processMessages(route.asJsonObject["triggerFulfillment"])?.let { messages ->
                        translationAgent.putFlow(PhrasePath(listOf(flowName, "", "condition", condition)), LanguageMessages(messages))
                    }
                }
                route.asJsonObject["intent"]?.asString?.let { intent ->
                    // If there are trigger fulfillment messages, capture them
                    processMessages(route.asJsonObject["triggerFulfillment"])?.let { messages ->
                        translationAgent.putFlow(PhrasePath(listOf(flowName, "", "intent", intent)), LanguageMessages(messages))
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
        // Walk all directories under the "flows" directory
        File("$rootPath/flows").listFiles()?.forEach { directory ->
            val flowPath = directory.absolutePath
            val flowName = directory.name // The directory name is the flow name
            // Walk all files under the "pages" directory
            File("$flowPath/pages").listFiles()?.forEach { file ->
                val pageName = file.name.removeSuffix(".json") // as in <page-name>.json minus .json
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                // If there are entry fulfillment messages, and they are not empty, capture them.
                jsonObject["entryFulfillment"]?.let { entryFulfillment ->
                    processMessages(entryFulfillment)?.let { messages ->
                        translationAgent.putPage(PhrasePath(listOf(flowName, pageName)), LanguageMessages(messages))
                    }
                }
                jsonObject["transitionRoutes"]?.asJsonArray?.forEach { route ->
                    route.asJsonObject["condition"]?.asString?.let { condition ->
                        // If there are transition route fulfillment messages, capture them
                        processMessages(route.asJsonObject["triggerFulfillment"])?.let { messages ->
                            translationAgent.putFlow(PhrasePath(listOf(flowName, pageName, "condition", condition)), LanguageMessages(messages))
                        }
                    }
                }
                jsonObject["form"]?.asJsonObject?.get("parameters")?.asJsonArray?.forEach { parameterElement ->
                    val parameter = parameterElement.asJsonObject
                    val displayName = parameter["displayName"].asString
                    parameter["fillBehavior"]?.asJsonObject?.let { fillBehavior ->
                        fillBehavior["initialPromptFulfillment"]?.let { initialPrompt ->
                            val messages = processMessages(initialPrompt)
                            if (!messages.isNullOrEmpty())
                                translationAgent.putPage(PhrasePath(listOf(flowName, pageName, "$displayName\ninitialPromptFulfillment")), LanguageMessages(messages))
                        }
                        fillBehavior["repromptEventHandlers"]?.asJsonArray?.forEach { event ->
                            val messages = processMessages(event.asJsonObject["triggerFulfillment"])
                            val eventName = event.asJsonObject["event"].asString
                            if (!messages.isNullOrEmpty())
                                translationAgent.putPage(PhrasePath(listOf(flowName, pageName, "$displayName\nrepromptEventHandlers\n$eventName")), LanguageMessages(messages))
                        }
                    }
                }
                // If the page has event handlers with fulfillment messages, capture them
                processEventHandlers(jsonObject, flowName).forEach { (event, languageMessages) ->
                    translationAgent.putPage(PhrasePath(listOf(flowName, pageName)), languageMessages)
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
    private fun processMessages(jsonElement: JsonElement, event: String? = null): Map<String, List<Message>>? {
        val messagesMap = mutableMapOf<String, MutableList<Message>>()

        fun addMessageToMap(languageCode: String, message: Message) {
            val messages = messagesMap.getOrPut(languageCode) { mutableListOf() }
            messages.add(message)
        }

        jsonElement.asJsonObject["messages"]?.asJsonArray?.forEach { element ->
            val languageCode = element.asJsonObject["languageCode"].asString
            val channel = element.asJsonObject["channel"]?.asString ?: "audio"

            processTextMessage(element, channel, event)?.let { message ->
                addMessageToMap(languageCode, message)
            }

            processPayloadMessage(element, channel, event)?.forEach { message ->
                addMessageToMap(languageCode, message)
            }
        }

        return messagesMap.takeIf { it.isNotEmpty() }
    }

    private fun processTextMessage(element: JsonElement, channel: String, event: String?): Message? {
        return element.asJsonObject["text"]?.asJsonObject?.get("text")?.asJsonArray?.map { it.asString }?.let { texts ->
            if (texts.isNotEmpty()) Message(texts, channel, "message", event) else null
        }
    }

    private fun processPayloadMessage(element: JsonElement, channel: String, event: String?): List<Message>? {
        val messages = mutableListOf<Message>()
        element.asJsonObject["payload"]?.asJsonObject?.get("richContent")?.asJsonArray?.forEach { outerList ->
            outerList.asJsonArray.forEach { richContentElement ->
                when (val elementType = richContentElement.asJsonObject["type"].asString) {
                    "chips" -> {
                        val chipsValues = richContentElement.asJsonObject["options"].asJsonArray.map {
                            var resultString = it.asJsonObject["text"].asString
                            val anchorUrl = it.asJsonObject["anchor"]?.asJsonObject?.get("href")?.asString
                            if (!anchorUrl.isNullOrEmpty()) {
                                resultString += " [$anchorUrl]"
                            }
                            resultString
                        }
                        messages.add(Message(chipsValues, channel, elementType, event))
                    }
                    "html" -> {
                        val htmlText = richContentElement.asJsonObject["html"].asString
                        messages.add(Message(listOf(htmlText), channel, elementType, event))
                    }
                    "button" -> {
                        val buttonText = richContentElement.asJsonObject["text"].asString
                        val buttonEvent = richContentElement.asJsonObject["event"].asJsonObject["event"].asString
                        messages.add(Message(listOf(buttonText), channel, elementType, buttonEvent))
                    }
                    "list" -> {
                        val listTitle = richContentElement.asJsonObject["title"].asString
                        val listSubtitle = richContentElement.asJsonObject["subtitle"]?.asString ?: ""
                        val listEvent = richContentElement.asJsonObject["event"].asJsonObject["event"].asString
                        val listTitles = listTitle.plus("\n".plus(listSubtitle))
                        messages.add(Message(listOf(listTitles.trim()), channel, elementType, listEvent))
                    }
                }
            }
        }
        // Return null if messages is empty, otherwise return the messages
        return if (messages.isEmpty()) null else messages
    }

    /**
     * Process event handlers and, if they have trigger fulfillment messages, extract them. Note that
     * we are treating the sys.no-match-default and sys.no-input-default as special cases - we capture
     * those for the Default Start Flow only, since we will reuse their fulfillment messages everywhere
     * when we import the agent.
     */
    private fun processEventHandlers(jsonElement: JsonElement, flowName: String) =
        jsonElement.asJsonObject["eventHandlers"]?.asJsonArray?.mapNotNull { handler ->
            val event = handler.asJsonObject["event"].asString
            val messages = processMessages(handler.asJsonObject["triggerFulfillment"], event)
            if (messages != null && (flowName == "Default Start Flow" ||
                        event != "sys.no-match-default" && event != "sys.no-input-default")
            )
                event to LanguageMessages(messages)
            else null
        }?.toMap() ?: mapOf()

}