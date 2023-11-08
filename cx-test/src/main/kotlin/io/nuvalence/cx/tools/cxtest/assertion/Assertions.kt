package io.nuvalence.cx.tools.cxtest.assertion

import com.google.cloud.dialogflow.cx.v3beta1.ResponseMessage
import io.nuvalence.cx.tools.cxtest.util.Properties
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.junit.jupiter.api.Assertions

enum class MatchingMode(val value: String) {
    NORMAL("normal") {
        override fun assertFuzzyMatchString(input: String, expected: String, actual: String, expectedRatio: Int) {
            val ratio = FuzzySearch.ratio(expected, actual)
            Assertions.assertTrue(
                ratio >= expectedRatio,
                "Trimmed string fuzzy match % ($ratio) below expected % ($expectedRatio).\nInput: \"$input\"\nExpected: \"$expected\"\nActual: \"$actual\""
            )
        }
    },
    ADAPTIVE("adaptive") {
        override fun assertFuzzyMatchString(input: String, expected: String, actual: String, expectedRatio: Int) {
            val newActual = actual.filterNot { char -> char.isWhitespace() }
            val ratio = FuzzySearch.ratio(expected, newActual)
            Assertions.assertTrue(
                ratio >= expectedRatio,
                "Trimmed string fuzzy match % ($ratio) below expected % ($expectedRatio).\nInput: \"$input\"\nExpected: \"$expected\"\nActual: \"$actual\""
            )
        }
    };

    /**
     * Asserts that the actual string matches the expected string with a fuzzy match.
     *
     * @param input the input string
     * @param expected the expected string
     * @param actual the actual string
     * @param expectedRatio the expected fuzzy match ratio
     */
    abstract fun assertFuzzyMatchString(input: String, expected: String, actual: String, expectedRatio: Int)

    companion object {
        infix fun from(value: String?): MatchingMode =
            MatchingMode.values().firstOrNull { matchingMode -> matchingMode.value == value } ?: NORMAL
    }
}

/**
 * Asserts that the actual string matches the expected string with a fuzzy match.
 *
 * @param input the input string
 * @param expected the expected string
 */
fun assertFuzzyMatch(input: String, expected: String, actual: List<ResponseMessage>) {
    val matchingMode = MatchingMode.from(Properties.MATCHING_MODE)
    val expectedRatio = Properties.MATCHING_RATIO

    val newActual = actual.joinToString("\n") { responseMessage ->
        if (responseMessage.outputAudioText.hasSsml()) responseMessage.outputAudioText.ssml; else ""
    }.let { stripSsml(it) }

    matchingMode.assertFuzzyMatchString(input, expected, newActual, expectedRatio)
}

/**
 * Strips SSML tags from a string.
 *
 * @param input the input string
 */
fun stripSsml(input: String): String {
    return input.replace(Regex("<.*?>"), "")
}