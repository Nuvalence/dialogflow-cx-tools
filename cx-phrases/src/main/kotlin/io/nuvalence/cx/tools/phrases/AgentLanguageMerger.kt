package io.nuvalence.cx.tools.phrases

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonWriter
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
        processFlows()
        processPages()
        processTransitionRouteGroups()
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
            val intentPath = directory.absolutePath
            val intentName = directory.name
            val languagePhrases = translationAgent.getIntent(PhrasePath(listOf(intentName)))
            languagePhrases?.phraseByLanguage?.keys?.forEach { languageCode ->
                val phrases = languagePhrases[languageCode] ?: listOf()
                val jsonElement = intentLanguage(languageCode, phrases)
                prettySave(jsonElement, "$intentPath/trainingPhrases/$languageCode.json")
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
            processEventHandlers(jsonObject, listOf(flowName, "event"), translationAgent::getFlow)
            jsonObject["transitionRoutes"].asJsonArray.forEach { route ->
                val condition = route.asJsonObject["condition"].asString
                val flow = translationAgent.getFlow(PhrasePath(listOf(flowName, "condition", condition)))
                if (flow != null) {
                    val entryFulfillment = route.asJsonObject["triggerFulfillment"].asJsonObject
                    val messages = languagePhrasesToJson(singleString = true, flow.phraseByLanguage)
                    entryFulfillment.remove("messages")
                    entryFulfillment.add("messages", messages)
                    processWebSiteParameter(entryFulfillment.asJsonObject["setParameterActions"])
                }
            }
            prettySave(jsonObject, "$flowPath/$flowName.json")
        }
    }

    /**
     * Replace fulfillments associated with pages. Those go to:
     *
     * <agent-root>/flows/<flow-name>/<flow-name>.json
     */
    private fun processPages() {
        File("$rootPath/flows").listFiles()?.forEach { directory ->
            val flowPath = directory.absolutePath
            val flowName = directory.name
            File("$flowPath/pages").listFiles()?.forEach { file ->
                val pageName = file.name.removeSuffix(".json")
                val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                processEventHandlers(jsonObject, listOf(flowName, pageName), translationAgent::getPages)
                val page = translationAgent.getPages(PhrasePath(listOf(flowName, pageName, "message")))
                if (page != null) {
                    val messages = languagePhrasesToJson(singleString = true, page.phraseByLanguage)
                    val entryFulfillment = jsonObject["entryFulfillment"].asJsonObject
                    entryFulfillment.remove("messages")
                    entryFulfillment.add("messages", messages)
                    processTransitionRoutes(jsonObject["transitionRoutes"]?.asJsonArray)
                }
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
                processTransitionRoutes(jsonObject["transitionRoutes"].asJsonArray)
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
            processWebSiteParameter(triggerFulfillment.asJsonObject["setParameterActions"])
        }
    }

    /**
     * Process event handlers, which may be found in different places of the agent. Note that for sys.no-match-default
     * and sys.no-input-default, we use the fulfillments associated with the Default Start Flow.
     */
    private fun processEventHandlers(jsonObject: JsonObject, pathPrefix: List<String>, getPhrases: (PhrasePath) -> LanguagePhrases?) {
        val eventsJson = jsonObject["eventHandlers"]?.asJsonArray
        eventsJson?.forEach { event ->
            val phrases = when (val eventName = event.asJsonObject["event"].asString) {
                "sys.no-match-default" -> getPhrases(PhrasePath(listOf("Default Start Flow", "event", "sys.no-match-default")))
                "sys.no-input-default" -> getPhrases(PhrasePath(listOf("Default Start Flow", "event", "sys.no-input-default")))
                else -> getPhrases(PhrasePath(pathPrefix + eventName))
            }
            if (phrases != null) {
                val jsonPhrases = languagePhrasesToJson(singleString = false, phrases.phraseByLanguage)
                val triggerFulfillment = event.asJsonObject["triggerFulfillment"].asJsonObject
                triggerFulfillment.remove("messages")
                triggerFulfillment.add("messages", jsonPhrases)
            }
        }
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