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

open class DFCXTestStep(
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

    constructor (userInput: String, expectedAgentOutput: String):
            this(userInput, expectedAgentOutput, "", "", "", listOf())

    override fun toString(): String {
        return "Step result: $result\nUser Input: $userInput \nExpected Agent Output: $expectedAgentOutput\nActual Agent Output: $actualAgentOutput\nExpected Page: $expectedPage\nActual Page: $actualPage\n"
    }
}

class DFCXInjectableTestStep(
    userInput: String,
    expectedAgentOutput: String,
    actualAgentOutput: String,
    expectedPage: String,
    actualPage: String,
    result: ResultLabel,
    diffs: List<TestRunDifference>,
    payloads: Map<String, String>)
    : DFCXTestStep (userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, result, diffs) {
    constructor (userInput: String, expectedAgentOutput: String, actualAgentOutput: String, expectedPage: String, actualPage: String, diffs: List<TestRunDifference>, payloads: Map<String, String>)
        : this(userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, ResultLabel.PASS, diffs, payloads)

    constructor (userInput: String, expectedAgentOutput: String, payloads: Map<String, String>)
        : this(userInput, expectedAgentOutput, "", "", "", listOf(), payloads)
}

open class DFCXTest <T: DFCXTestStep>(
    val testCaseId: String,
    val testCaseName: String,
    val tags: List<String>,
    val notes: String,
    var result: ResultLabel,
    val resultSteps: MutableList<T>,
    val diffs: List<TestRunDifference>
) {
    constructor (testCaseId: String, testCaseName: String, tags: List<String>, notes: String) :
            this(testCaseId, testCaseName, tags, notes,
                ResultLabel.PASS, mutableListOf<T>(), listOf())

    constructor (testCaseId: String, testCaseName: String, tags: List<String>, notes: String, resultSteps: MutableList<T>) :
            this(testCaseId, testCaseName, tags, notes,
                ResultLabel.PASS, resultSteps, listOf()
            )

    override fun toString(): String {
        return "Test Case Name: $testCaseName, Result: $result, Result Steps:\n${resultSteps.joinToString("\n"){ resultStep -> resultStep.toString() }}"
    }
}

class DFCXInjectableTest(
    testCaseId: String,
    testCaseName: String,
    tags: List<String>,
    notes: String,
    result: ResultLabel,
    resultSteps: MutableList<DFCXInjectableTestStep>,
    diffs: List<TestRunDifference>,
    val ssn: String)
    : DFCXTest<DFCXInjectableTestStep>(testCaseId, testCaseName, tags, notes, result, resultSteps, diffs) {

    constructor (testCaseId: String, testCaseName: String, tags: List<String>, notes: String, ssn: String) :
            this(testCaseId, testCaseName, tags, notes,
                ResultLabel.PASS, mutableListOf<DFCXInjectableTestStep>(), listOf(), ssn)

    constructor (testCaseId: String, testCaseName: String, tags: List<String>, notes: String, resultSteps: MutableList<DFCXInjectableTestStep>, ssn: String) :
            this(testCaseId, testCaseName, tags, notes,
                ResultLabel.PASS, resultSteps, listOf(), ssn)
}
