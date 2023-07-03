package io.nuvalence.cx.tools.phrases

import com.google.gson.JsonElement
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
                        translationAgent.putFlow(PhrasePath(listOf(flowName, "", "condition", condition)), LanguagePhrases(messages))
                    }
                }
                route.asJsonObject["intent"]?.asString?.let { intent ->
                    // If there are trigger fulfillment messages, capture them
                    processMessages(route.asJsonObject["triggerFulfillment"])?.let { messages ->
                        translationAgent.putFlow(PhrasePath(listOf(flowName, "", "intent", intent)), LanguagePhrases(messages))
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
                    val messages = processMessages(entryFulfillment)
                    if (messages != null)
                        translationAgent.putPage(PhrasePath(listOf(flowName, pageName, "message")), LanguagePhrases(messages))
                }
                jsonObject["transitionRoutes"]?.asJsonArray?.forEach { route ->
                    route.asJsonObject["condition"]?.asString?.let { condition ->
                        // If there are transition route fulfillment messages, capture them
                        processMessages(route.asJsonObject["triggerFulfillment"])?.let { messages ->
                            translationAgent.putFlow(PhrasePath(listOf(flowName, pageName, "condition", condition)), LanguagePhrases(messages))
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
                                translationAgent.putPage(PhrasePath(listOf(flowName, pageName, "$displayName\ninitialPromptFulfillment")), LanguagePhrases(messages))
                        }
                        fillBehavior["repromptEventHandlers"]?.asJsonArray?.forEach { event ->
                            val messages = processMessages(event.asJsonObject["triggerFulfillment"])
                            val eventName = event.asJsonObject["event"].asString
                            if (!messages.isNullOrEmpty())
                                translationAgent.putPage(PhrasePath(listOf(flowName, pageName, "$displayName\nrepromptEventHandlers\n$eventName")), LanguagePhrases(messages))
                        }
                    }
                }
                // If the page has event handlers with fulfillment messages, capture them
                processEventHandlers(jsonObject, flowName).forEach { (event, messages) ->
                    translationAgent.putPage(PhrasePath(listOf(flowName, pageName, event)), messages)
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
    private fun processMessages(jsonElement: JsonElement) =
        jsonElement.asJsonObject["messages"]?.asJsonArray?.mapNotNull { element ->
            val languageCode = element.asJsonObject["languageCode"].asString
            val outerText = element.asJsonObject["text"]
            val texts = outerText?.asJsonObject?.get("text")?.asJsonArray?.map { text ->
                text.asString
            }
            if (texts != null) languageCode to texts else null
        }?.toMap()

    /**
     * Process event handlers and, if they have trigger fulfillment messages, extract them. Note that
     * we are treating the sys.no-match-default and sys.no-input-default as special cases - we capture
     * those for the Default Start Flow only, since we will reuse their fulfillment messages everywhere
     * when we import the agent.
     */
    private fun processEventHandlers(jsonElement: JsonElement, flowName: String) =
        jsonElement.asJsonObject["eventHandlers"]?.asJsonArray?.mapNotNull { handler ->
            val event = handler.asJsonObject["event"].asString
            val messages = processMessages(handler.asJsonObject["triggerFulfillment"])
            if (messages != null && (flowName == "Default Start Flow" ||
                        event != "sys.no-match-default" && event != "sys.no-input-default")
            )
                event to LanguagePhrases(messages)
            else null
        }?.toMap() ?: mapOf()

}