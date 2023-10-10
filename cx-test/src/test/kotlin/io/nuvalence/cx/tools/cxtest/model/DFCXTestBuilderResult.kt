package io.nuvalence.cx.tools.cxtest.model

enum class ResultLabel(private val value: String) {
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
    var result: ResultLabel
) {
    constructor (userInput: String, expectedAgentOutput: String, actualAgentOutput: String, expectedPage: String, actualPage: String):
        this(userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, ResultLabel.PASS)

    override fun toString(): String {
        return "Step result: $result\nUser Input: $userInput \nExpected Agent Output: $expectedAgentOutput\nActual Agent Output: $actualAgentOutput\nExpected Page: $expectedPage\nActual Page: $actualPage\n"
    }
}

data class DFCXTestBuilderResult(
    val testCaseId: String,
    val testCaseName: String,
    val tags: List<String>,
    val note: String,
    var result: ResultLabel,
    val resultSteps: MutableList<DFCXTestBuilderResultStep>
) {
    constructor (testCaseId: String, testCaseName: String, tags: List<String>, note: String) :
        this(testCaseId, testCaseName, tags, note,
            ResultLabel.PASS, mutableListOf<DFCXTestBuilderResultStep>())

    override fun toString(): String {
        return "Test Case Name: $testCaseName, Result: $result, Result Steps:\n${resultSteps.joinToString("\n"){ resultStep -> resultStep.toString() }}"
    }
}
