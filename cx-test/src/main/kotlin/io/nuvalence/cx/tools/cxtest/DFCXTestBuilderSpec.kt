package io.nuvalence.cx.tools.cxtest

import com.google.cloud.dialogflow.cx.v3.*
import io.nuvalence.cx.tools.cxtest.assertion.isNoDateMatch
import io.nuvalence.cx.tools.cxtest.extension.DFCXTestBuilderExtension
import io.nuvalence.cx.tools.cxtest.model.test.DFCXTestBuilderResult
import io.nuvalence.cx.tools.cxtest.model.test.DFCXTestBuilderResultStep
import io.nuvalence.cx.tools.cxtest.model.test.ResultLabel
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

@Execution(ExecutionMode.SAME_THREAD)
@Tag("dfcx")
@ExtendWith(DFCXTestBuilderExtension::class)
class DFCXTestBuilderSpec {
    companion object {
        val formattedResults: ThreadLocal<MutableMap<String, DFCXTestBuilderResult>> = ThreadLocal()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DFCXTestBuilderExtension::class)
    @Synchronized
    fun testCases(displayName: String, testCase: TestCase, testCaseResult: TestCaseResult) {
        val testBuilderResult = DFCXTestBuilderResult(
            testCaseId = testCase.name,
            testCaseName = testCase.displayName,
            tags = testCase.tagsList.map { tag -> tag.toString() },
            notes = testCase.notes
        )

        val fullResult = DFCXTestBuilderExtension.testClient.getTestCaseResult(testCaseResult.name)

        fun getInput (turn: ConversationTurn): String {
            return if (turn.userInput.input.dtmf.digits.isNotEmpty()) {
                "[DTMF] ${turn.userInput.input.dtmf.digits}${if (turn.userInput.input.dtmf.finishDigit.isNotEmpty()) "|${turn.userInput.input.dtmf.finishDigit}" else ""}"
            } else if (turn.userInput.input.event.event.isNotEmpty()) {
                "[EVENT] ${turn.userInput.input.event.event}"
            } else {
                turn.userInput.input.text.text
            }
        }

        fullResult.conversationTurnsList.forEachIndexed { index, turn ->
            val input = getInput(turn)
            val resultStep = DFCXTestBuilderResultStep(
                userInput = input,
                expectedAgentOutput = testCase.testCaseConversationTurnsList[index].virtualAgentOutput.textResponsesList.joinToString("\n") { responseMessage ->
                    responseMessage.textList.reduce { acc, text -> acc + text }
                },
                actualAgentOutput = turn.virtualAgentOutput.textResponsesList.joinToString("\n") { responseMessage ->
                    responseMessage.textList.reduce { acc, text -> acc + text }
                },
                expectedPage = testCase.testCaseConversationTurnsList[index].virtualAgentOutput.currentPage.displayName,
                actualPage = turn.virtualAgentOutput.currentPage.displayName,
                diffs = turn.virtualAgentOutput.differencesList
            )

            if (turn.virtualAgentOutput.differencesList.any { diff -> diff.type == TestRunDifference.DiffType.PAGE }) {
                resultStep.result = ResultLabel.FAIL
            }

            if (turn.virtualAgentOutput.differencesList.any { diff -> diff.type == TestRunDifference.DiffType.UTTERANCE}) {
                if (!isNoDateMatch(resultStep.expectedAgentOutput, resultStep.actualAgentOutput)) {
                    resultStep.result = ResultLabel.FAIL
                }
            }

            testBuilderResult.resultSteps.add(resultStep)
        }

        // Test failures should also include text mismatches that otherwise count as passes
        if(testCaseResult.testResult == TestResult.FAILED || testBuilderResult.resultSteps.any { resultStep ->
            resultStep.result == ResultLabel.FAIL
        }) {
            testBuilderResult.result = ResultLabel.FAIL
        }

        synchronized(formattedResults) {
            formattedResults.get()[testBuilderResult.testCaseId] = testBuilderResult

            if (testBuilderResult.result == ResultLabel.FAIL) {
                Assertions.fail<AssertionError>("Test failed")
            }
        }
    }
}