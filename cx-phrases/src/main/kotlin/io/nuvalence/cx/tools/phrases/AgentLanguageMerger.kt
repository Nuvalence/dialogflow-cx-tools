package io.nuvalence.cx.tools.phrases

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonWriter
import io.nuvalence.cx.tools.shared.zipDirectory
import java.io.File
import java.io.StringWriter

/**
 * Merges the source agent with the changes from the spreadsheet. This is a merge process, where the source
 * agent was copied to the target directory, and then we merge the language changes to it. This way we
 * preserve the agent's original contents, and overwrite the phrases with the information from the spreadsheet.
 *
 * The process is similar to export - we walk the agent structure and find the places where there are
 * phrases; the difference is that instead of exporting them to the spreadsheet, we merge the changes
 * from the spreadsheet into the agent.
 *
 * If you look at the AgentPhraseExtractor class, you will recognize the same visitation patterns
 * here.
 *
 * Note the big assumption here - the structure of the agent cannot change between an import/export.
 */
class AgentLanguageMerger(private val translationAgent: TranslationAgent, private val rootPath: String) {
    fun process() {
        processAgent()
        processIntents()
        processEntityTypes()
        processFlows()
        processPages()
        processTransitionRouteGroups()
        zipDirectory(rootPath, "$rootPath.zip")
    }

    /**
     * Update the agent with the default and supported languages
     */
    private fun processAgent() {
        val jsonObject = JsonParser.parseString(File("$rootPath/agent.json").readText()).asJsonObject
        jsonObject.addProperty("defaultLanguageCode", translationAgent.defaultLanguageCode)
        jsonObject.add("supportedLanguageCodes", toJsonArray(translationAgent.supportedLanguageCodes))
        prettySave(jsonObject, "$rootPath/agent.json")
    }

    /**
     * Replace the intent training phrases with the ones from the spreadsheet. Those go to:
     *
     * <agent-root>/intents/phrases/<language-code>.json
     */
    private fun processIntents() {
        File("$rootPath/intents").listFiles()?.forEach { directory ->
            val intentName = directory.name
            val intentPath = directory.absolutePath

            // Ensure the trainingPhrases directory exists
            val trainingPhrasesPath = "$intentPath/trainingPhrases"

            val trainingPhrasesDir = File(trainingPhrasesPath)
            if (!trainingPhrasesDir.exists()) {
                trainingPhrasesDir.mkdirs()
            }

            val languageMessages = translationAgent.getIntent(PhrasePath(listOf(intentName)))?.messagesByLanguage
            languageMessages?.forEach { (language, messages) ->
                val phrasesSet = messages.flatMap { it.phrases ?: emptyList() }.toMutableSet()
                val file = File(trainingPhrasesDir, "$language.json")

                if (!file.exists()) {
                    file.createNewFile()
                }

                val originalTrainingPhrases = if (file.readText().isNotBlank()) {
                    JsonParser.parseString(file.readText()).asJsonObject
                } else {
                    JsonObject().apply { add("trainingPhrases", JsonArray()) }
                }

                val outputTrainingPhrases = JsonArray()

                originalTrainingPhrases["trainingPhrases"].asJsonArray.forEach { phrase ->
                    val parts = phrase.asJsonObject["parts"].asJsonArray
                    val combinedText = combineParts(parts)


                    if (combinedText in phrasesSet) {
                        outputTrainingPhrases.add(phrase)
                        phrasesSet.remove(combinedText) // Remove the text from the set
                    }
                }
                phrasesSet.forEach { phrase ->
                    if (phrase.isNotEmpty()) {
                        outputTrainingPhrases.addAll(intentLanguage(language, listOf(phrase)))
                    }
                }

                val resultJsonObject = JsonObject().apply { add("trainingPhrases", outputTrainingPhrases)}
                prettySave(resultJsonObject, file.absolutePath)
            }
        }
    }

    /**
     * Combine the different parts of the training phrase to build the full phrase.
     */
    private fun combineParts(parts: JsonArray): String = parts.joinToString("") { part ->
        part.asJsonObject.run {
            val text = this["text"].asString
            this["parameterId"]?.asString?.let { "[$text]($it)" } ?: text
        }
    }

    /**
     * Replace the entity types and their synonyms with the contents of the spreadsheet. Those go to:
     *
     * <agent-root>/entityTypes/<entity-name>/entities/<language-code>.json
     */
    private fun  processEntityTypes() {
        File("$rootPath/entityTypes").listFiles()?.forEach { directory ->
            val entityPath = directory.absolutePath
            val entityName = directory.name // the directory name is the entity name
            val entityKind = JsonParser.parseString(File("$entityPath/$entityName.json").readText()).asJsonObject["kind"].asString

            val entites = translationAgent.getEntities(entityName)
            val languagePhrases = mutableMapOf<String, MutableMap<String, List<String>>>()
            entites?.forEach { (value, phraseByLanguage) ->
                phraseByLanguage.phraseByLanguage.forEach { (languageCode, phrases) ->
                    val valueMap = languagePhrases.getOrPut(languageCode) { mutableMapOf() }
                    valueMap[value] = phrases
                }
            }
            languagePhrases.forEach { (languageCode, phrases) ->
                if (languageCode.isNotBlank() && languageCode.length < 6) {

                    val entitiesJsonArray = JsonArray()
                    phrases.forEach { (value, synonyms) ->
                        if (synonyms.isNotEmpty() && synonyms.all { it.isNotBlank() }) {
                            val entity = JsonObject()
                            val synonymArray = JsonArray()
                            if (entityKind == "KIND_REGEXP") {
                                synonymArray.add(synonyms[0])
                                entity.addProperty("value", synonyms[0])
                            } else {
                                synonyms.forEach { synonym -> synonymArray.add(synonym) }
                                entity.addProperty("value", value)
                            }

                            entity.add("synonyms", synonymArray)
                            entity.addProperty("languageCode", languageCode)
                            entitiesJsonArray.add(entity)
                        }
                    }

                    if (entitiesJsonArray.size() > 0) {
                        val entityJsonObject = JsonObject()
                        entityJsonObject.add("entities", entitiesJsonArray)

                        prettySave(entityJsonObject, "$entityPath/entities/$languageCode.json")
                    }
                }
            }
        }
    }

    /**
     * Replace fulfillments associated with event handlers and transition routes associated with conditions.
     * Those go to:
     *
     * <agent-root>/flows/<flow-name>/<flow-name>.json
     */
    private fun processFlows() {
        File("$rootPath/flows").listFiles()?.forEach { directory ->
            val flowPath = directory.absolutePath
            val flowName = directory.name
            val jsonObject = JsonParser.parseString(File("$flowPath/$flowName.json").readText()).asJsonObject
            processEventHandlers_NEW(jsonObject, listOf(flowName, "", "event"), translationAgent::getFlow)
            jsonObject["transitionRoutes"].asJsonArray.forEach { route ->
                processTransitionRoute(flowName, route, "condition")
                processTransitionRoute(flowName, route, "intent")
            }
            prettySave(jsonObject, "$flowPath/$flowName.json")
        }
    }

    /**
     * Process transition by intent or by condition.
     */
    private fun processTransitionRoute(flowName: String, route: JsonElement, transitionTrigger: String) {
        route.asJsonObject["triggerFulfillment"].asJsonObject["messages"]?.asJsonArray?.forEach { message ->
            val channel = message.asJsonObject["channel"]?.asString ?: "audio"
            val isText = message.asJsonObject.has("text")
            val type = if (isText) {
                "message"
            } else if (message.asJsonObject.has("payload")) {
                message.asJsonObject["payload"].asJsonObject["richContent"].asJsonArray[0].asJsonArray[0].asJsonObject["type"].asString
            } else {
                "unknown"
            }
            route.asJsonObject[transitionTrigger]?.asString?.let { entry ->
                translationAgent.getFlow(PhrasePath(listOf(flowName, "", transitionTrigger, entry, type, channel)))?.let { flow ->
                    val entryFulfillment = route.asJsonObject["triggerFulfillment"].asJsonObject
                    replaceMessages(entryFulfillment, languagePhrasesToJson(singleString = false, flow.messagesByLanguage))
                    processParameters(entryFulfillment.asJsonObject)
                }
            }
        }
    }

    /**
     * Replace fulfillments associated with pages. Those go to:
     *
     * <agent-root>/flows/<flow-name>/<page-name>.json
     */
    private fun processPages() {
        File("$rootPath/flows").listFiles()?.forEach { directory ->
            val flowPath = directory.absolutePath
            val flowName = directory.name
            File("$flowPath/pages").listFiles()?.forEach { file ->
                val pageName = file.name.removeSuffix(".json")
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                processEventHandlers_NEW(jsonObject, listOf(flowName, pageName), translationAgent::getPages)

                val entryFulfillment = jsonObject["entryFulfillment"]?.asJsonObject
                entryFulfillment?.get("messages")?.asJsonArray?.forEach { message ->
                    val channel = message.asJsonObject["channel"]?.asString ?: "audio"
                    val isText = message.asJsonObject.has("text")
                    val type = if (isText) {
                        "message"
                    } else if (message.asJsonObject.has("payload")) {
                        message.asJsonObject["payload"].asJsonObject["richContent"].asJsonArray[0].asJsonArray[0].asJsonObject["type"].asString
                    } else {
                        "unknown"
                    }
                    translationAgent.getPages(PhrasePath(listOf(flowName, pageName)))?.let { page ->
                        val replacementMessages = languagePhrasesToJson(singleString = true, page.messagesByLanguage)
                        replaceMessages(entryFulfillment, replacementMessages)
                        val webhook = entryFulfillment.get("webhook")
                        val tags = entryFulfillment.get("tag")
                        entryFulfillment.remove("webhook")
                        entryFulfillment.remove("tag")
                        if (webhook != null && webhook !is JsonNull) {
                            entryFulfillment.add("webhook", webhook)
                        }
                        if (tags != null && tags !is JsonNull) {
                            entryFulfillment.add("tag", tags)
                        }
                        processParameters(entryFulfillment)
                        processTransitionRoutes(jsonObject["transitionRoutes"]?.asJsonArray)
                    }
                }
                val transitionRoutes = jsonObject["transitionRoutes"]?.asJsonArray
                transitionRoutes?.forEach { route ->
                    val triggerFulfillment = route.asJsonObject["triggerFulfillment"].asJsonObject
                    triggerFulfillment["messages"]?.asJsonArray?.forEach { message ->
                        val channel = message.asJsonObject["channel"]?.asString ?: "audio"
                        val isText = message.asJsonObject.has("text")
                        val type = if (isText) {
                            "message"
                        } else if (message.asJsonObject.has("payload")) {
                            message.asJsonObject["payload"].asJsonObject["richContent"].asJsonArray[0].asJsonArray[0].asJsonObject["type"].asString
                        } else {
                            "unknown"
                        }

                        route.asJsonObject["condition"]?.asString?.let { condition ->
                            translationAgent.getFlow(PhrasePath(listOf(flowName, pageName, "condition", condition, type, channel)))?.let { phrases ->
                                replaceMessages(triggerFulfillment, languagePhrasesToJson(singleString = false, phrases.messagesByLanguage))
                                processParameters(triggerFulfillment.asJsonObject)
                            }
                        }
                    }
                }
                val form = jsonObject["form"]?.asJsonObject
                form?.get("parameters")?.asJsonArray?.forEach { parameterElement ->
                    val parameter = parameterElement.asJsonObject
                    val displayName = parameter["displayName"].asString
                    parameter["fillBehavior"]?.asJsonObject?.let { fillBehavior ->
                        fillBehavior["initialPromptFulfillment"]?.asJsonObject?.let { initialPrompt ->
                            initialPrompt["messages"]?.asJsonArray?.forEach { message ->
                                val channel = message.asJsonObject["channel"]?.asString ?: "audio"
                                val isText = message.asJsonObject.has("text")
                                val type = if (isText) {
                                    "message"
                                } else if (message.asJsonObject.has("payload")) {
                                    message.asJsonObject["payload"].asJsonObject["richContent"].asJsonArray[0].asJsonArray[0].asJsonObject["type"].asString
                                } else {
                                    "unknown"
                                }
                                translationAgent.getPages(PhrasePath(listOf(flowName, pageName, "$displayName\ninitialPromptFulfillment")))?.let { phrases ->
                                    replaceMessages(initialPrompt, languagePhrasesToJson(singleString = true, phrases.messagesByLanguage))
                                }
                            }
                        }
                        fillBehavior["repromptEventHandlers"]?.asJsonArray?.forEach { eventElement ->
                            eventElement.asJsonObject.let { event ->
                                val eventName = event["event"].asString
                                val triggerFulfillment = event["triggerFulfillment"].asJsonObject
                                triggerFulfillment["messages"]?.asJsonArray?.forEach { message ->
                                    val channel = message.asJsonObject["channel"]?.asString ?: "audio"
                                    val isText = message.asJsonObject.has("text")
                                    val type = if (isText) {
                                        "message"
                                    } else if (message.asJsonObject.has("payload")) {
                                        message.asJsonObject["payload"].asJsonObject["richContent"].asJsonArray[0].asJsonArray[0].asJsonObject["type"].asString
                                    } else {
                                        "unknown"
                                    }
                                    translationAgent.getPages(PhrasePath(listOf(flowName, pageName, "$displayName\nrepromptEventHandlers\n$eventName")))?.let { phrases ->
                                        replaceMessages(triggerFulfillment, languagePhrasesToJson(singleString = false, phrases.messagesByLanguage))
                                    }
                                }
                            }
                        }
                    }
                }
//                translationAgent.getPages(PhrasePath(listOf(flowName, pageName, "chips")))?.let { page ->
//                    val entryFulfillment = jsonObject["entryFulfillment"].asJsonObject
//                    val messages = entryFulfillment.get("messages").asJsonArray
////                    val messagesWithChips = chipsTextToJson(page.messagesByLanguage)
////                    messagesWithChips.forEach { message ->
////                        if (message !in messages) {
////                            messages.add(message)
////                        }
////                    }
//                    replaceMessages(entryFulfillment, messages)
//                    processParameters(entryFulfillment)
//                    processTransitionRoutes(jsonObject["transitionRoutes"]?.asJsonArray)
//                }
                prettySave(jsonObject, "$flowPath/pages/$pageName.json")
            }
        }
    }

    /**
     * Process transition route groups. Those go to:
     *
     * <agent-root>/flows/<flow-name>/transitionRouteGroups/<transition-route-group-name>
     */
    private fun processTransitionRouteGroups() {
        File("$rootPath/flows").listFiles()?.forEach { directory ->
            val flowPath = directory.absolutePath
            File("$flowPath/transitionRouteGroups").listFiles()?.forEach { file ->
                val rgPath = file.absolutePath
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                processTransitionRoutes(jsonObject["transitionRoutes"]?.asJsonArray)
                prettySave(jsonObject, rgPath)
            }
        }
    }

    /**
     * Process transition routes - if the fulfillment has a website parameter, create the
     * appropriate prosody for it.
     */
    private fun processTransitionRoutes(transitionRoutes: JsonArray?) {
        transitionRoutes?.forEach { jsonElement ->
            val triggerFulfillment = jsonElement.asJsonObject["triggerFulfillment"]
            processParameters(triggerFulfillment.asJsonObject)
        }
    }

    /**
     * Process event handlers, which may be found in different places of the agent. Note that for sys.no-match-default
     * and sys.no-input-default, we use the fulfillments associated with the Default Start Flow.
     */
    private fun processEventHandlers_NEW(jsonObject: JsonObject, pathPrefix: List<String>, getPhrases: (PhrasePath) -> LanguageMessages?) {
        val eventsJson = jsonObject["eventHandlers"]?.asJsonArray
        eventsJson?.forEach { event ->
            val triggerFulfillment = event.asJsonObject["triggerFulfillment"].asJsonObject
            triggerFulfillment["messages"]?.asJsonArray?.forEach { message ->
                val channel = message.asJsonObject["channel"]?.asString ?: "audio"
                val isText = message.asJsonObject.has("text")
                val type = if (isText) {
                    "message"
                } else if (message.asJsonObject.has("payload")) {
                    message.asJsonObject["payload"].asJsonObject["richContent"].asJsonArray[0].asJsonArray[0].asJsonObject["type"].asString
                } else {
                    "unknown"
                }
                val phrases = when (val eventName = event.asJsonObject["event"].asString) {
                    "sys.no-match-default" -> getPhrases(PhrasePath(listOf("Default Start Flow", "", "event", "sys.no-match-default")))
                    "sys.no-input-default" -> getPhrases(PhrasePath(listOf("Default Start Flow", "", "event", "sys.no-input-default")))
                    else -> getPhrases(PhrasePath(pathPrefix + eventName))
                }
                if (phrases != null) {
                    val replacementMessages = languagePhrasesToJson(singleString = false, phrases.messagesByLanguage)
                    replaceMessages(triggerFulfillment, replacementMessages)
                    processParameters(triggerFulfillment)
                }
            }
        }
    }

    /**
     * Replaces the "messages" entry under a JsonObject
     */
    private fun replaceMessages(jsonObject: JsonObject, messages: JsonArray) {
        jsonObject.remove("messages")
        jsonObject.add("messages", messages)
    }

    /**
     * Saves a JSON element with formatting and two space indentation.
     */
    private fun prettySave(jsonElement: JsonElement, path: String) {
        val stringWriter = StringWriter()
        val jsonWriter = JsonWriter(stringWriter)
        jsonWriter.isLenient = true
        jsonWriter.isHtmlSafe = true
        jsonWriter.setIndent("  ")
        Streams.write(jsonElement, jsonWriter)
        File(path).writeText(stringWriter.toString())
    }

}