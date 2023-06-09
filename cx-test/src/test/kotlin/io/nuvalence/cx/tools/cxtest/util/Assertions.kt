package io.nuvalence.cx.tools.cxtest.util

import com.google.cloud.dialogflow.cx.v3beta1.ResponseMessage
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

    abstract fun assertFuzzyMatchString(input: String, expected: String, actual: String, expectedRatio: Int)

    companion object {
        infix fun from(value: String): MatchingMode =
            MatchingMode.values().firstOrNull { matchingMode -> matchingMode.value == value } ?: NORMAL
    }
}

fun assertFuzzyMatch(input: String, expected: String, actual: List<ResponseMessage>) {
    val matchingMode = MatchingMode.from(PROPERTIES.MATCHING_MODE.get())
    val expectedRatio = PROPERTIES.MATCHING_RATIO.get().toInt()

    val newActual = actual.joinToString("\n") { responseMessage ->
        if (responseMessage.text.textCount > 0) responseMessage.text.getText(0); else ""
    }

    matchingMode.assertFuzzyMatchString(input, expected, newActual, expectedRatio)
}
