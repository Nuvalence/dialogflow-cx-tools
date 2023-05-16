package io.nuvalence.cx.tools.phrases

/**
 * Convenience enum with the different types of phrases, and the name we should
 * use for the spreadsheet tab.
 */
enum class PhraseType(val title: String) {
    Intents("Training Phrases"),
    Flows("Transitions"),
    Pages("Fulfillments");
}

/**
 * Holds a list of phrases for the supported languages
 */
data class LanguagePhrases(val phraseByLanguage: Map<String, List<String>>) {
    /**
     * Given an order for the languages (we like the default one to be first),
     * flattens the associated strings into a single newline separated string.
     */
    fun flatten(order: List<String>) =
        order.map { language ->
            phraseByLanguage[language]?.joinToString("\n") ?: ""
        }
    operator fun get(languageCode: String) = phraseByLanguage[languageCode]
}

/**
 * Holds the language phrases associated with the different paths. A path is what
 * allows us to associate a row in the spreadsheet with the set of phrases for
 * each language.
 */
class TranslationPhrases {
    private val phrases = mutableMapOf<PhrasePath, LanguagePhrases>()

    operator fun get(phrasePath: PhrasePath) = phrases[phrasePath]

    fun put(path: PhrasePath, languagePhrases: LanguagePhrases) {
        phrases[path] = languagePhrases
    }

    /**
     * Flatten the information, converting it into a list of list of strings,
     * which is convenient for exporting to a spreadsheet. Each row contains
     * the path plus one entry per language.
     */
    fun flatten(order: List<String>) =
        phrases.map { (path, languagePhrases) ->
            path.path + languagePhrases.flatten(order)
        }
}

/**
 * This class holds all the phrases, broken down by intent/flow/pages/path/language.
 */
class TranslationAgent(val defaultLanguageCode: String, val supportedLanguageCodes: List<String>) {
    private val intents = TranslationPhrases()
    private val flows = TranslationPhrases()
    private val pages = TranslationPhrases()
    val allLanguages = listOf(defaultLanguageCode) + supportedLanguageCodes

    fun getIntent(path: PhrasePath) = intents[path]
    fun getFlow(path: PhrasePath) = flows[path]
    fun getPages(path: PhrasePath) = pages[path]

    fun putIntent(path: PhrasePath, languagePhrases: LanguagePhrases) = intents.put(path, languagePhrases)
    fun putFlow(path: PhrasePath, languagePhrases: LanguagePhrases) = flows.put(path, languagePhrases)
    fun putPage(path: PhrasePath, languagePhrases: LanguagePhrases) = pages.put(path, languagePhrases)

    fun flattenIntents() = intents.flatten(allLanguages)
    fun flattenFlows() = flows.flatten(allLanguages)
    fun flattenPages() = pages.flatten(allLanguages)
}

/**
 * As a data class, so we can use it as key of a map
 */
data class PhrasePath(val path: List<String>)
