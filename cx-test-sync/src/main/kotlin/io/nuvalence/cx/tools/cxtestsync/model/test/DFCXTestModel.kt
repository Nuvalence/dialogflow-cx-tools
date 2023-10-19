package io.nuvalence.cx.tools.cxtestsync.model.test

enum class ResultLabel(private val value: String) {
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
    var result: ResultLabel?
) {
    constructor (userInput: String, expectedAgentOutput: String, actualAgentOutput: String, expectedPage: String, actualPage: String):
            this(userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, ResultLabel.PASS)

    constructor (userInput: String, expectedAgentOutput: String):
            this(userInput, expectedAgentOutput, "", "", "")

    override fun toString(): String {
        return "Step result: $result\nUser Input: $userInput \nExpected Agent Output: $expectedAgentOutput\nActual Agent Output: $actualAgentOutput\nExpected Page: $expectedPage\nActual Page: $actualPage\n"
    }
}

class DFCXInjectableTestStep(userInput: String, expectedAgentOutput: String, actualAgentOutput: String, expectedPage: String, actualPage: String, result: ResultLabel?, payloads: Map<String, String>)
    : DFCXTestStep (userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, result) {
    constructor (userInput: String, expectedAgentOutput: String, actualAgentOutput: String, expectedPage: String, actualPage: String, payloads: Map<String, String>)
        : this(userInput, expectedAgentOutput, actualAgentOutput, expectedPage, actualPage, ResultLabel.PASS, payloads)

    constructor (userInput: String, expectedAgentOutput: String, payloads: Map<String, String>)
        : this(userInput, expectedAgentOutput, "", "", "", payloads)
}

open class DFCXTest <T: DFCXTestStep>(
    val testCaseId: String,
    val testCaseName: String,
    val tags: List<String>,
    val notes: String,
    var result: ResultLabel?,
    val resultSteps: MutableList<T>
) {
    constructor (testCaseId: String, testCaseName: String, tags: List<String>, note: String) :
            this(testCaseId, testCaseName, tags, note,
                ResultLabel.PASS, mutableListOf<T>())

    constructor (testCaseId: String, testCaseName: String, tags: List<String>, note: String, resultSteps: MutableList<T>) :
            this(testCaseId, testCaseName, tags, note,
                ResultLabel.PASS, resultSteps)

    override fun toString(): String {
        return "Test Case Name: $testCaseName, Result: $result, Result Steps:\n${resultSteps.joinToString("\n"){ resultStep -> resultStep.toString() }}"
    }
}

class DFCXInjectableTest(testCaseId: String, testCaseName: String, tags: List<String>, notes: String, result: ResultLabel?, resultSteps: MutableList<DFCXInjectableTestStep>, val ssn: String)
    : DFCXTest<DFCXInjectableTestStep>(testCaseId, testCaseName, tags, notes, result, resultSteps) {

    constructor (testCaseId: String, testCaseName: String, tags: List<String>, notes: String, ssn: String) :
            this(testCaseId, testCaseName, tags, notes,
                ResultLabel.PASS, mutableListOf<DFCXInjectableTestStep>(), ssn)

    constructor (testCaseId: String, testCaseName: String, tags: List<String>, notes: String, resultSteps: MutableList<DFCXInjectableTestStep>, ssn: String) :
            this(testCaseId, testCaseName, tags, notes,
                ResultLabel.PASS, resultSteps, ssn)
}
