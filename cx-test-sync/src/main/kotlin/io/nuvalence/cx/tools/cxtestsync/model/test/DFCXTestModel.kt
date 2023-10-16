package io.nuvalence.cx.tools.cxtestsync.model.test

enum class ResultLabel(private val value: String) {
    PASS("PASS"),
    WARN("WARN"),
    FAIL("FAIL");

    override fun toString(): String {
        return value
    }
}

data class DFCXTestStep(
    val userInput: String,
    val expectedAgentOutput: String,
    val actualAgentOutput: String,
    val expectedPage: String,
    val actualPage: String,
    var result: ResultLabel
) {
    constructor (userInput: String, expectedAgentOutput: String, actualAgentOutput: String, expectedPage: String, actualPage: String):
            this(userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, ResultLabel.PASS)

    constructor (userInput: String, expectedAgentOutput: String, result: ResultLabel):
            this(userInput, expectedAgentOutput, "", "", "", result)

    override fun toString(): String {
        return "Step result: $result\nUser Input: $userInput \nExpected Agent Output: $expectedAgentOutput\nActual Agent Output: $actualAgentOutput\nExpected Page: $expectedPage\nActual Page: $actualPage\n"
    }
}

data class DFCXTest(
    val testCaseId: String,
    val testCaseName: String,
    val tags: List<String>,
    val notes: String,
    var result: ResultLabel,
    val resultSteps: MutableList<DFCXTestStep>
) {
    constructor (testCaseId: String, testCaseName: String, tags: List<String>, note: String) :
            this(testCaseId, testCaseName, tags, note,
                ResultLabel.PASS, mutableListOf<DFCXTestStep>())

    constructor (testCaseId: String, testCaseName: String, tags: List<String>, note: String, resultSteps: MutableList<DFCXTestStep>) :
            this(testCaseId, testCaseName, tags, note,
                ResultLabel.PASS, resultSteps)

    override fun toString(): String {
        return "Test Case Name: $testCaseName, Result: $result, Result Steps:\n${resultSteps.joinToString("\n"){ resultStep -> resultStep.toString() }}"
    }
}
