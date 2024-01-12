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

data class Message(val phrases: List<String>?, val channel: String? = null, val type: String? = null) {
    fun flatten(): String {
        val result = phrases?.joinToString("\n") ?: ""
        return result
    }
}

/**
 * Holds a list of phrases for the supported languages
 */
data class LanguageMessages(val messagesByLanguage: Map<String, List<Message>>) {
    /**
     * Given an order for the languages (we like the default one to be first),
     * flattens the associated strings into a single newline separated string.
     */
    // TODO: Modify this to return flattened string within the Message object with type and channel to start each new line (if it isn't null)
//    fun flatten(order: List<String>): List<List<List<String?>>?> {
//        var testMap: MutableMap<String, Message>
//        val result = order.map { language ->
//            messagesByLanguage[language]?.map { message: Message ->
//                listOf(message.type, message.channel, message.phrases?.joinToString("\n") ?: "")
//            }
//        }
//
//        return result
//    }

    operator fun get(languageCode: String) = messagesByLanguage[languageCode]
}

// TODO: Update LanguagePhrases constructor to val phraseByLanguage: Map<String, Message>
data class LanguagePhrases(val phraseByLanguage: Map<String, List<String>>) {
    /**
     * Given an order for the languages (we like the default one to be first),
     * flattens the associated strings into a single newline separated string.
     */
    // TODO: Modify this to return flattened string within the Message object with type and channel to start each new line (if it isn't null)
    fun flatten(order: List<String>): List<String> {
        val result = order.map { language ->
            phraseByLanguage[language]?.joinToString("\n") ?: ""
        }
        return result
    }
    operator fun get(languageCode: String) = phraseByLanguage[languageCode]
}

/**
 * Holds the language phrases associated with the different paths. A path is what
 * allows us to associate a row in the spreadsheet with the set of phrases for
 * each language.
 */

class TranslationPhrases_NEW {
    private val phrases = mutableMapOf<PhrasePath, LanguageMessages>()

    operator fun get(phrasePath: PhrasePath) = phrases[phrasePath]

    operator fun set(path: PhrasePath, languageMessages: LanguageMessages) {
        val existingMessages = phrases[path]

        phrases[path] = if (existingMessages == null) {
            languageMessages
        } else {
            val combinedMessagesByLanguage = existingMessages.messagesByLanguage.toMutableMap()

            languageMessages.messagesByLanguage.forEach { (languageCode, newMessages) ->
                combinedMessagesByLanguage.merge(languageCode, newMessages) { existingMessages, newMessages ->
                    existingMessages + newMessages
                }
            }

            LanguageMessages(combinedMessagesByLanguage)
        }
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
                        phrasePath.add(message.type)
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
    fun flatten(order: List<String>): List<List<String>> {
        val result = phrases.map { (path, languagePhrases) ->
            path.path + languagePhrases.flatten(order)
        }
        // You can now debug or inspect the 'result' variable here
        return result
    }
}

/**
 * Holds the different entityTypes. They are accessed by their display name, value and language.
 */
class TranslationEntities {
    private  val entities = mutableMapOf<String, MutableMap<String, LanguagePhrases>>()

    // TODO: Modify this to return the List<String> texts
    operator fun get(displayName: String, value: String, language: String) =
        entities[displayName]?.get(value)?.get(language) ?: error("Agent structure changed: displayName = $displayName value = $value language = $language")

    operator fun set(displayName: String, value: String, language: String, synonyms: List<String>) {
        val entityType = entities.getOrPut(displayName) { mutableMapOf() }
        val phrases = entityType[value]
        if (phrases == null)
            // TODO: Update this to convert synonyms to Message objects prior to the map
            entityType[value] = LanguagePhrases(mapOf(language to synonyms))
        else
            // TODO: Update this to convert synonyms to Message objects priot to the map
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
//    private val intents = TranslationPhrases()
    private val intents_NEW = TranslationPhrases_NEW()
//    private val flows = TranslationPhrases()
    private val flows_NEW = TranslationPhrases_NEW()
//    private val pages = TranslationPhrases()
    private val pages_NEW = TranslationPhrases_NEW()
    val allLanguages = listOf(defaultLanguageCode) + supportedLanguageCodes

    fun getEntity(displayName: String, value: String, language: String) = entities.get(displayName, value, language)
    fun getIntent(path: PhrasePath) = intents_NEW[path]
    fun getFlow(path: PhrasePath) = flows_NEW[path]
    fun getPages(path: PhrasePath) = pages_NEW[path]

    fun putEntity(displayName: String, value: String, language: String, synonyms: List<String>) = entities.set(displayName, value, language, synonyms)
    fun putIntent(path: PhrasePath, languageMessages: LanguageMessages) = intents_NEW.set(path, languageMessages)
//    fun putIntent_OLD(path: PhrasePath, languagePhrases: LanguagePhrases) = intents.set(path, languagePhrases)
//    fun putFlow_OLD(path: PhrasePath, languagePhrases: LanguagePhrases) = flows.set(path, languagePhrases)
    fun putFlow(path: PhrasePath, languageMessages: LanguageMessages) = flows_NEW.set(path, languageMessages)
//    fun putPage(path: PhrasePath, languagePhrases: LanguagePhrases) = pages.set(path, languagePhrases)
    fun putPage(path: PhrasePath, languageMessages: LanguageMessages) = pages_NEW.set(path, languageMessages)

    fun flattenEntities() = entities.flatten(allLanguages)
//    fun flattenIntents() = intents.flatten(allLanguages)
    fun flattenIntents_NEW() = intents_NEW.flatten(allLanguages)
//    fun flattenFlows() = flows.flatten(allLanguages)
    fun flattenFlows_NEW() = flows_NEW.flatten(allLanguages)
//    fun flattenPages() = pages.flatten(allLanguages)
    fun flattenPages_NEW() = pages_NEW.flatten(allLanguages)
}

/**
 * As a data class, so we can use it as key of a map
 */
data class PhrasePath(val path: List<String>)
