package io.nuvalence.cx.tools.phrases

import com.google.gson.JsonObject
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.getValue
import java.io.File

/**
 * Helper functions to generate SSML tags and audio prosody.
 */

const val START_SPEAK = "<speak>"
const val END_SPEAK = "</speak>"

const val START_PROSODY_RATE = """<break time="300ms"/><prosody rate="90%">"""
const val BREAK_100_MS = """<break time="100ms"/>"""
const val END_PROSODY_RATE = """</prosody><break time="300ms"/>"""

const val START_SAY_VERBATIM = """<s><break time="100ms"/><say-as interpret-as="verbatim">"""
const val END_SAY_VERBATIM = """</say-as></s>"""
const val END_SAY = "</say-as>"

/**
 * Read config file from which regex values will be read
 */
val configFile = File("cx-phrases/config.conf")
val config: Config = ConfigFactory.parseString(configFile.readText().trimMargin())

/**
 * Used to separate URL components
 */
val URL_SEPARATORS: Set<String> by config

/**
 * Finds and matches URLs
 */
val MATCH_URL_REGEX: Regex by config

/**
 * Finds and matches 10-digit phone numbers with dashes
 */
val MATCH_PHONE_REGEX: Regex by config

/**
 * Finds and matches percentages with or without decimals
 */
val MATCH_PERCENTAGE_REGEX: Regex by config

/**
 * Finds and matches numbers, but ignore time (e.g. 14:29pm) and phone numbers since we want to
 * say those as is/they were already processed.
 */
val MATCH_NUMBERS_REGEX: Regex by config

/**
 * These tokens will not be spelled out
 */
val SHORT_TOKEN_WHITELIST: Set<String> by config

/**
 * These tokens are specifically spelled out
 */
val URL_VERBATIM_TOKENS: Set<String> by config

/**
 * Consonant sequences that sound weird in English, so we revert to spelling out the word instead
 * of saying it as-is.
 */
val INVALID_CONSONANT_SEQUENCE: Regex by config

/**
 * List of custom regex matchers found in config file to run and replace
 */
val CUSTOM_MATCH_REPLACE_LIST: Set<Map<String, String>> by config

/**
 * Boolean indicating if we are processing URLs based on English or other languages
 */
var LANGUAGE_CODE = "en"
/**
 * Generates the outputAudioText element
 *
 * @param languageCode the language code (e.g. en or es)
 * @param phrase the text phrase to convert
 */
fun audioMessage(languageCode: String, phrase: String): JsonObject {
    LANGUAGE_CODE = languageCode
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
    val replacedPercentage = processString(replacedPhone, MATCH_PERCENTAGE_REGEX, ::processPercentage)
    val replacedNumbers = processString(replacedPercentage, MATCH_NUMBERS_REGEX, ::processNumber)
    val replacedUrls = processString(replacedNumbers, MATCH_URL_REGEX, ::processUrl)
    val replacedWebSite = replacedUrls
        .replace("\$session.params.web-site", "\$session.params.web-site-ssml")
        .replace("\$session.params.web-site-fwd", "\$session.params.web-site-fwd-ssml")
    var replacedCustom = replacedWebSite
    CUSTOM_MATCH_REPLACE_LIST.forEach {     // loop through custom_match_replace_list rules
        var languages = it.getValue("languages").split(",")
        if (LANGUAGE_CODE in languages || "all" in languages) { // if language matches the language of the rule or "all", then run it
            replacedCustom = processString(
                replacedCustom,
                Regex(it.getValue("match").toString()),
                fun(_: String) = it.getValue("replace").toString())
        }
    }
    return "$START_SPEAK\n$replacedCustom\n$END_SPEAK"
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
        if (phrase.matches(Regex(">\\s*$toProcess\\s*<"))) {
            return phrase
        }
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
            if (LANGUAGE_CODE == "en") {    // if English, we want to make sure we process certain words in URLs differently
                if (token in URL_SEPARATORS) {  // . - / are said as-is
                    "$BREAK_100_MS$START_SAY_VERBATIM $token $END_SAY_VERBATIM"
                } else if (token.length < 4) { // Short tokens have special treatment
                    if (token in SHORT_TOKEN_WHITELIST) // These are read as-is (e.g. "com")
                        token
                    else // Otherwise we spell it. Stupid say-as verbatim or character not always work...
                        "$START_SAY_VERBATIM ${token.map { it }.joinToString(" ")} $END_SAY_VERBATIM"
                } else {
                    if (token.contains(INVALID_CONSONANT_SEQUENCE) || token.lowercase() in URL_VERBATIM_TOKENS) // If the token has weird consonant sequences or is specifically listed in our spelling dictionary, spell it
                        "$START_SAY_VERBATIM ${token.map { it }.joinToString(" ")} $END_SAY_VERBATIM"
                    else
                        token
                }
            } else {    // if other languages, just spell out URL tokens
                "$START_SAY_VERBATIM ${token.map { it }.joinToString(" ")} $END_SAY_VERBATIM"
            }
        } + END_PROSODY_RATE
    else url

fun processNumber(number: String) =
    // if a number has more than 3 digits, say one digit at a time, and make sure we say "zero", not "oh"
    (if (number.length > 4)
        START_PROSODY_RATE + START_SAY_VERBATIM + number + END_SAY_VERBATIM + END_PROSODY_RATE
    else
        number
    )

/**
 * Adds prosody rate and percentage interpretation to percentages
 */
fun processPercentage(number: String) =
    START_PROSODY_RATE + // Pause and talk slowly
        """<say-as interpret-as="percentage">""" +
            number + END_SAY + END_PROSODY_RATE

/**
 * Adds prosody rate and telephone interpretation to phone numbers
 */
fun processPhone(number: String) =
    START_PROSODY_RATE + // Pause and talk slowly
        """<say-as interpret-as="telephone" google:style="zero-as-zero">""" +
            number.trimEnd() + END_SAY + END_PROSODY_RATE + " "

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

