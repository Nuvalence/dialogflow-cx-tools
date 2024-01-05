package io.nuvalence.cx.tools.phrases

/**
 * Convenience enum with the different types of phrases, and the name we should
 * use for the spreadsheet tab.
 */
enum class PhraseType(val title: String) {
    Intents("Training Phrases"),
    Entities("Entities"),
    Flows("Transitions"),
    Pages("Fulfillments");
}

/**
 * Holds a list of phrases for the supported languages
 */
data class LanguagePhrasesOld(val phraseByLanguage: Map<String, List<String>>) {
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
data class LanguagePhrases(val phraseByLanguage: List<AgentPhrasesExtractor.Fulfillment>) {
    // Secondary constructor that accepts a Map<String, List<String>>
    constructor(phraseByLanguage: Map<String, List<String>>) : this(
        phraseByLanguage.map { (language, phrases) ->
            AgentPhrasesExtractor.Fulfillment(language, phrases)
        }
    )

    /**
     * Given an order for the languages (we like the default one to be first),
     * flattens the associated strings into a single newline separated string.
     */
    fun flatten(order: List<String>): List<String> =
        order.map { languageCode ->
            phraseByLanguage.filter { it.language == languageCode }
                .flatMap { it.phrases }
                .joinToString("\n")
        }
    operator fun get(languageCode: String) = phraseByLanguage.filter { it.language == languageCode }
}

/**
 * Holds the language phrases associated with the different paths. A path is what
 * allows us to associate a row in the spreadsheet with the set of phrases for
 * each language.
 */
class TranslationPhrases {
    private val phrases = mutableMapOf<PhrasePath, LanguagePhrases>()

    operator fun get(phrasePath: PhrasePath) = phrases[phrasePath]

    operator fun set(path: PhrasePath, languagePhrases: LanguagePhrases) {
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
 * Holds the different entityTypes. They are accessed by their display name, value and language.
 */
class TranslationEntities {
    private  val entities = mutableMapOf<String, MutableMap<String, LanguagePhrases>>()

    operator fun get(displayName: String, value: String, language: String) =
        entities[displayName]?.get(value)?.get(language) ?: error("Agent structure changed: displayName = $displayName value = $value language = $language")

    operator fun set(displayName: String, value: String, language: String, synonyms: List<String>) {
        val entityType = entities.getOrPut(displayName) { mutableMapOf() }
        val phrases = entityType[value]
        if (phrases == null)
            entityType[value] = LanguagePhrases(mapOf(language to synonyms))
        else
            entityType[value] = LanguagePhrases(mapOf(language to synonyms) + phrases.phraseByLanguage.map { it.language to it.phrases }.toMap())
    }

    fun flatten(order: List<String>) =
        entities.flatMap { (displayName, entityMap) ->
            entityMap.map { (entity, languagePhrases) ->
                listOf(entity) + languagePhrases.flatten(order)
            }.map { list ->
                listOf(displayName) + list
            }
        }
}

/**
 * This class holds all the phrases, broken down by intent/flow/pages/path/language.
 */
class TranslationAgent(val defaultLanguageCode: String, val supportedLanguageCodes: List<String>) {
    private val entities = TranslationEntities()
    private val intents = TranslationPhrases()
    private val flows = TranslationPhrases()
    private val pages = TranslationPhrases()
    val allLanguages = listOf(defaultLanguageCode) + supportedLanguageCodes

    fun getEntity(displayName: String, value: String, language: String) = entities.get(displayName, value, language)
    fun getIntent(path: PhrasePath) = intents[path]
    fun getFlow(path: PhrasePath) = flows[path]
    fun getPages(path: PhrasePath) = pages[path]

    fun putEntity(displayName: String, value: String, language: String, synonyms: List<String>) = entities.set(displayName, value, language, synonyms)
    fun putIntent(path: PhrasePath, languagePhrases: LanguagePhrases) = intents.set(path, languagePhrases)
    fun putFlow(path: PhrasePath, languagePhrases: LanguagePhrases) = flows.set(path, languagePhrases)
    fun putPage(path: PhrasePath, languagePhrases: LanguagePhrases) = pages.set(path, languagePhrases)

    fun flattenEntities() = entities.flatten(allLanguages)
    fun flattenIntents() = intents.flatten(allLanguages)
    fun flattenFlows() = flows.flatten(allLanguages)
    fun flattenPages() = pages.flatten(allLanguages)
}

/**
 * As a data class, so we can use it as key of a map
 */
data class PhrasePath(val path: List<String>)
