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

data class Message(
    val phrases: List<String>?,
    val channel: String? = null,
    val type: String? = null,
    val event: String? = null
)

/**
 * Holds a list of phrases for the supported languages
 */
data class LanguageMessages(val messagesByLanguage: Map<String, List<Message>>) {
    operator fun get(languageCode: String) = messagesByLanguage[languageCode]
}

data class LanguagePhrases(val phraseByLanguage: Map<String, List<String>>) {
    /**
     * Given an order for the languages (we like the default one to be first),
     * flattens the associated strings into a single newline separated string.
     */
    fun flatten(order: List<String>): List<String> {
        return order.map { language ->
            phraseByLanguage[language]?.joinToString("\n") ?: ""
        }
    }
    operator fun get(languageCode: String) = phraseByLanguage[languageCode]
}

/**
 * Holds the language phrases associated with the different paths. A path is what
 * allows us to associate a row in the spreadsheet with the set of phrases for
 * each language.
 */
class TranslationPhrases {
    private val phrases = mutableMapOf<PhrasePath, LanguageMessages>()

    operator fun get(phrasePath: PhrasePath) = phrases[phrasePath]

    operator fun set(path: PhrasePath, languageMessages: LanguageMessages) {
        val combinedMessagesByLanguage = phrases[path]?.messagesByLanguage.orEmpty().toMutableMap()
        languageMessages.messagesByLanguage.forEach { (languageCode, newMessages) ->
            combinedMessagesByLanguage.merge(languageCode, newMessages) { existing, new ->
                existing + new
            }
        }
        phrases[path] = LanguageMessages(combinedMessagesByLanguage)
    }

    /**
     * Flatten the information, converting it into a list of list of strings,
     * which is convenient for exporting to a spreadsheet. Each row contains
     * the path plus one entry per language.
     */
    fun flatten(order: List<String>): List<List<String>> {
        val newPathToMessage = mutableMapOf<PhrasePath, MutableList<String>>()

        // Flatten the phrases for each path and language.
        phrases.forEach { (path, languagePhrases) ->
            order.forEach { language ->
                languagePhrases.messagesByLanguage[language]?.forEach { message: Message ->
                    // Create a unique string representation of the path.
                    val phrasePath: MutableList<String> = path.path.toMutableList()
                    if (!message.type.isNullOrEmpty()) {
                        if (!message.event.isNullOrEmpty() && !phrasePath.contains(message.event)) {
                            phrasePath.add(listOf(message.type, message.event).joinToString("\n"))
                        } else {
                            phrasePath.add(message.type)
                        }
                    }
                    if (!message.channel.isNullOrEmpty()) {
                        phrasePath.add(message.channel)
                    }
                    // Get or create the list for this path and add the new message to it.
                    val list = newPathToMessage.getOrPut(PhrasePath(phrasePath)) { mutableListOf() }
                    list.add(message.phrases?.joinToString("\n") ?: "")
                }
            }
        }

        // Transform the map into the required List<List<String>> format.
        val result = newPathToMessage.map { (path, messages) ->
            path.path + messages  // Combine the path with its messages into a single list.
        }

        return result
    }


}

/**
 * Holds the different entityTypes. They are accessed by their display name, value and language.
 */
class TranslationEntities {
    private  val entities = mutableMapOf<String, MutableMap<String, LanguagePhrases>>()

    operator fun get(displayName: String, value: String, language: String) =
        entities[displayName]?.get(value)?.get(language) ?: error("Agent structure changed: displayName = $displayName value = $value language = $language")

    operator fun get(displayName: String) = entities[displayName]

    operator fun set(displayName: String, value: String, language: String, synonyms: List<String>) {
        val entityType = entities.getOrPut(displayName) { mutableMapOf() }
        val phrases = entityType[value]
        if (phrases == null)
            entityType[value] = LanguagePhrases(mapOf(language to synonyms))
        else
            entityType[value] = LanguagePhrases(mapOf(language to synonyms) + phrases.phraseByLanguage)
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

    fun getEntities(displayName: String) = entities.get(displayName)
    fun getIntent(path: PhrasePath) = intents[path]
    fun getFlow(path: PhrasePath) = flows[path]
    fun getPages(path: PhrasePath) = pages[path]

    fun putEntity(displayName: String, value: String, language: String, synonyms: List<String>) = entities.set(displayName, value, language, synonyms)
    fun putIntent(path: PhrasePath, languageMessages: LanguageMessages) = intents.set(path, languageMessages)
    fun putFlow(path: PhrasePath, languageMessages: LanguageMessages) = flows.set(path, languageMessages)
    fun putPage(path: PhrasePath, languageMessages: LanguageMessages) = pages.set(path, languageMessages)

    fun flattenEntities() = entities.flatten(allLanguages)
    fun flattenIntents() = intents.flatten(allLanguages)
    fun flattenFlows() = flows.flatten(allLanguages)
    fun flattenPages() = pages.flatten(allLanguages)
}

/**
 * As a data class, so we can use it as key of a map
 */
data class PhrasePath(val path: List<String>)
