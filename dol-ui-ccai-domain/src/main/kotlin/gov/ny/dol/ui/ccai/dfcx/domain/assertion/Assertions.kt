package gov.ny.dol.ui.ccai.dfcx.domain.assertion

import com.google.cloud.dialogflow.cx.v3beta1.ResponseMessage
import io.nuvalence.cx.tools.cxtestcore.Properties
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
        infix fun from(value: String?): MatchingMode =
            MatchingMode.values().firstOrNull { matchingMode -> matchingMode.value == value } ?: NORMAL
    }
}

fun assertFuzzyMatch(input: String, expected: String, actual: List<ResponseMessage>) {
    val matchingMode = MatchingMode.from(Properties.getProperty<String>("matchingMode"))
    val expectedRatio = Properties.getProperty<Int>("matchingRatio")

    val newActual = actual.joinToString("\n") { responseMessage ->
        if (responseMessage.outputAudioText.hasSsml()) responseMessage.outputAudioText.ssml; else ""
    }.let { stripSsml(it) }

    matchingMode.assertFuzzyMatchString(input, expected, newActual, expectedRatio)
}

fun stripSsml(input: String): String {
    return input.replace(Regex("<.*?>"), "")
}