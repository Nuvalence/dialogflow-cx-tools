package io.nuvalence.cx.tools.phrases

import com.google.gson.JsonObject

/**
 * Helper functions to generate SSML tags and audio prosody.
 */

const val START_SPEAK = "<speak>"
const val END_SPEAK = "</speak>"

const val START_PROSODY_RATE = """<break time="300ms"/><prosody rate="90%">"""
const val BREAK_100_MS = """<break time="100ms"/>"""
const val END_PROSODY_RATE = """</prosody><break time="300ms"/>"""

const val START_SAY_VERBATIM = """<say-as interpret-as="verbatim">"""
const val END_SAY = "</say-as>"

/**
 * Used to separate URL components
 */
val URL_SEPARATORS = setOf("#", "/", ".", "_", "-")

/**
 * Finds and matches URLs
 */
val MATCH_URL_REGEX = Regex("\\s\\w+\\.\\w+(?:[.\\/\\-]\\w+)*\\b")

/**
 * Finds and matches 10-digit phone numbers with dashes
 */
val MATCH_PHONE_REGEX = Regex("\\b(\\d{3}-\\d{3}-\\d{4})\\b")

/**
 * Finds and matches numbers, but ignore time (e.g. 14:29pm) since we want to
 * say those as is.
 */
val MATCH_NUMBERS_REGEX = Regex("\\b(?<!\\d{3}-\\d{3}-)\\b\\d+\\b(?!-\\d{3}-\\d{4})(?!-\\d{1,4})(?!%\">|:|-)\\b")

/**
 * These tokens will not be spelled out
 */
val SHORT_TOKEN_WHITELIST = setOf("com", "org", "gov")

/**
 * Consonant sequences that sound weird in English, so we revert to spelling out the word instead
 * of saying it as-is.
 */
val INVALID_CONSONANT_SEQUENCE = Regex("\\b.*([^aeiouy]{5,}|[^aeiou][^aeiouy]{4,}[^aeiou]|[hjmnqvwxz]r|[cdfghjqvwxz]s|[dhjmnqrtvxwz]l|sth|kpy|ww|hh|jj|kk|qq|vv|xx).*\\b")

/**
 * Generates the outputAudioText element
 *
 * @param languageCode the language code (e.g. en or es)
 * @param phrase the text phrase to convert
 */
fun audioMessage(languageCode: String, phrase: String): JsonObject {
    val ssml = JsonObject()
    ssml.addProperty("ssml", addSsmlTags(phrase))
    val audio = JsonObject()
    audio.add("outputAudioText", ssml)
    audio.addProperty("languageCode", languageCode)
    return audio
}

/**
 * Adds the SSML tags to a phrase
 *
 * @param phrase the phrase to convert
 */
fun addSsmlTags(phrase: String): String {
    if (phrase.contains(START_SPEAK) && phrase.contains(END_SPEAK))
        return phrase // Because prosody has already been defined in the fulfillment - just use what's there
    val replacedPhone = processString(phrase, MATCH_PHONE_REGEX, ::processPhone)
    val replacedNumbers = processString(replacedPhone, MATCH_NUMBERS_REGEX, ::processNumber)
    val replacedUrls = processString(replacedNumbers, MATCH_URL_REGEX, ::processUrl)
    val replacedWebSite = replacedUrls
        .replace("\$session.params.web-site", "\$session.params.web-site-ssml")
        .replace("\$session.params.web-site-fwd", "\$session.params.web-site-fwd-ssml")
    return "$START_SPEAK\n$replacedWebSite\n$END_SPEAK"
}

/**
 * Helper function to reduce code duplication - it finds tokens that match the supplied
 * regular expression, and replaces them with the return value of the function passed
 * as parameter.
 *
 * @param phrase the phrase to process.
 * @param regex the regular expression to match and extract tokens
 * @param replace the function that replaces the capture group with the new value
 */
fun processString(phrase: String, regex: Regex, replace: (String) -> String): String {
    val parts = StringBuilder()
    var lastEndIndex = 0
    regex.findAll(phrase).forEach { matchResult ->
        val text = phrase.substring(lastEndIndex, matchResult.range.first)
        parts.append(text)
        val toProcess = matchResult.groupValues[0]
        parts.append(replace(toProcess))
        lastEndIndex = matchResult.range.last + 1
    }
    val text = phrase.substring(lastEndIndex).trim()
    if (text.isNotEmpty())
        parts.append(text)
    return parts.toString()
}

/**
 * Given a URL token, apply the appropriate prosody.
 */
fun processUrl(url: String) =
    if (url != "session.params.web-site" && url != "session.params.web-site-fwd") // We don't want to change those
        START_PROSODY_RATE + // Pause and talk slowly
        splitUrl(url).joinToString("") { token ->
            if (token in URL_SEPARATORS) {  // . - / are said as-is
                "$BREAK_100_MS$START_SAY_VERBATIM $token $END_SAY"
            } else if (token.length < 4) { // Short tokens have special treatment
                if (token in SHORT_TOKEN_WHITELIST) // These are read as-is (e.g. "com")
                    token
                else // Otherwise we spell it. Stupid say-as verbatim or character not always work...
                    "$START_SAY_VERBATIM ${token.map { it }.joinToString(" ")} $END_SAY"
            } else {
                if (token.contains(INVALID_CONSONANT_SEQUENCE)) // If the token has weird consonant sequences, spell it
                    "$START_SAY_VERBATIM ${token.map { it }.joinToString(" ")} $END_SAY"
                else
                    token
            }
        } + END_PROSODY_RATE
    else url

fun processNumber(number: String) =
    START_PROSODY_RATE + // Pause and talk slowly
    (if (number == "800")  // Special case for phone numbers
        " eight hundred "
    else if (number.length < 4)
            number
         else // Otherwise, say one digit at a time, and make sure we say "zero", not "oh"
            number.map { if (it == '0') "zero" else it }.joinToString(" ")) + END_PROSODY_RATE

fun processPhone(number: String) =
    START_PROSODY_RATE + // Pause and talk slowly
        """<say-as interpret-as="telephone">""" +
            number + END_SAY + END_PROSODY_RATE

/**
 * Splits a URL in its basic components, including the separators (e.g. . or /)
 */
fun splitUrl(url: String): List<String> {
    var trimmedUrl = url.trim()
    val tokens = mutableListOf<StringBuilder>()
    tokens.add(StringBuilder())
    trimmedUrl.forEach { c ->
        if (c.toString() in URL_SEPARATORS) {
            if (tokens.last().isEmpty()) {
                tokens.last().append(c)
            } else {
                tokens.add(StringBuilder())
                tokens.last().append(c)
            }
            tokens.add(StringBuilder())
        } else {
            tokens.last().append(c)
        }
    }
    return tokens.map { sb ->
        sb.toString()
    }
}

