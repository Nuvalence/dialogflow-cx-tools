package io.nuvalence.cx.tools.phrases.util

import io.nuvalence.cx.tools.phrases.util.PhraseType.*
import io.nuvalence.cx.tools.shared.SheetReader
import java.net.URL

/**
 * Extracts phrases from an exported sheet and create an instance of TranslationAgent.
 *
 * @param credentialsURL URL pointing to where the credentials.json file is (e.g.
 * file:///path/to/credentials.json)
 * @param spreadsheetId the ID of the Google Sheet (details below).
 */
class SheetPhrasesExtractor(private val credentialsURL: URL, private val spreadsheetId: String) {
    fun processSheet(): TranslationAgent {
        val translationAgent = processIntents()
        processEntityTypes(translationAgent)
        processFlows(translationAgent)
        processPages(translationAgent)
        return translationAgent
    }

    /**
     * Read the tab containing the intents training phrases, and create an instance
     * of TranslationAgent that will hold the rest of the phrases.
     */
    private fun processIntents(): TranslationAgent {
        val intents = SheetReader(credentialsURL, spreadsheetId, Intents.title).read()
        val defaultLanguageCode = intents.first()[1] // Position 1 is the default language
        // Other supported languages
        val supportedLanguageCodes = intents.first().mapIndexed { index, language ->
            if (index > 1) language else null
        }.mapNotNull { it }
        val translationAgent = TranslationAgent(defaultLanguageCode, supportedLanguageCodes)
        processRows(translationAgent,  intents, 1, translationAgent::putIntent)
        return translationAgent
    }

    /**
     * Read the tab containing the entity types - those are the ones that come from
     * <agent-root>/entityTypes/<entity-name>/entities, with one file per language.
     */
    private fun processEntityTypes(translationAgent: TranslationAgent) {
        val entities = SheetReader(credentialsURL, spreadsheetId, Entities.title).read()
        processRows(translationAgent, entities, 2, translationAgent::putEntity)
    }

    /**
     * Read the tab containing the flow-related phrases - those are the ones that came from
     * <agent root>/flows/<flow-name>/<flow-name>.json
     */
    private fun processFlows(translationAgent: TranslationAgent) {
        val flows = SheetReader(credentialsURL, spreadsheetId, Flows.title).read()
        processRows(translationAgent, flows, 6, translationAgent::putFlow)
    }

    /**
     * Read the tab containing the page-related phrases - those are the ones that came form
     * <agent root>/flows/<flow-name>/pages/<page-name>.json
     */
    private fun processPages(translationAgent: TranslationAgent) {
        val pages = SheetReader(credentialsURL, spreadsheetId, Pages.title).read()
        processRows(translationAgent, pages, 2, translationAgent::putPage)
    }

    /**
     * All rows follow the same pattern: first set of columns have the path to the phrase,
     * and then we have one column per language, with the header representing the language code.
     *
     * @param translationAgent to store the phrases
     * @param rows the list of rows to process
     * @param put function that stores the processed phrase
     */
    private fun processRows(
        translationAgent: TranslationAgent,
        rows: List<List<String>>,
        pathSize: Int,
        put: (PhrasePath, LanguageMessages) -> Unit
    ) {
        // Assume the first row contains headers
        val headers = rows.first()

        // Determine if "Type" and "Channel" columns exist
        val typeIndex = headers.indexOf("Type").takeIf { it != -1 }
        val channelIndex = headers.indexOf("Channel").takeIf { it != -1 }
        val englishLanguageIndex = headers.indexOf("en")

        // Process each row except the first (header) row
        rows.drop(1).forEach { row ->
            val path = row.take(pathSize)
            val phrases = translationAgent.allLanguages.zip(row.drop(englishLanguageIndex)).associate { (language, texts) ->
                val type = typeIndex?.let { row[it] }
                val channel = channelIndex?.let { row[it] }
                val phrases = texts.split('\n')
                language to listOf(Message(phrases, channel, type, null))
            }
            put(PhrasePath(path), LanguageMessages(phrases))
        }
    }
}