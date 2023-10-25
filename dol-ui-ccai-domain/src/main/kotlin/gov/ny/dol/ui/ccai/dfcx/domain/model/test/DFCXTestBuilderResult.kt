package gov.ny.dol.ui.ccai.dfcx.domain.model.test

import com.google.cloud.dialogflow.cx.v3.TestRunDifference

enum class ResultLabel(val value: String) {
    PASS("PASS"),
    WARN("WARN"),
    FAIL("FAIL");

    override fun toString(): String {
        return value
    }
}

data class DFCXTestBuilderResultStep(
    val userInput: String,
    val expectedAgentOutput: String,
    val actualAgentOutput: String,
    val expectedPage: String,
    val actualPage: String,
    var result: ResultLabel,
    val diffs: List<TestRunDifference>
) {
    constructor (userInput: String, expectedAgentOutput: String, actualAgentOutput: String, expectedPage: String, actualPage: String, diffs: List<TestRunDifference>):
        this(userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, ResultLabel.PASS, diffs)

    override fun toString(): String {
        return "Step result: $result\nUser Input: $userInput \nExpected Agent Output: $expectedAgentOutput\nActual Agent Output: $actualAgentOutput\nExpected Page: $expectedPage\nActual Page: $actualPage\n"
    }
}

data class DFCXTestBuilderResult(
    val testCaseId: String,
    val testCaseName: String,
    val tags: List<String>,
    val notes: String,
    var result: ResultLabel,
    val resultSteps: MutableList<DFCXTestBuilderResultStep>
) {
    constructor (testCaseId: String, testCaseName: String, tags: List<String>, notes: String) :
        this(testCaseId, testCaseName, tags, notes,
            ResultLabel.PASS, mutableListOf<DFCXTestBuilderResultStep>())

    override fun toString(): String {
        return "Test Case Name: $testCaseName, Result: $result, Result Steps:\n${resultSteps.joinToString("\n"){ resultStep -> resultStep.toString() }}"
    }
}
