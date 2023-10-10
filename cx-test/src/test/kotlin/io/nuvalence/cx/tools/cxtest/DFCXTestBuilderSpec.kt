package io.nuvalence.cx.tools.cxtest

import com.google.cloud.dialogflow.cx.v3.*
import io.nuvalence.cx.tools.cxtest.extension.DFCXTestBuilderExtension
import io.nuvalence.cx.tools.cxtest.model.DFCXTestBuilderResult
import io.nuvalence.cx.tools.cxtest.model.DFCXTestBuilderResultStep
import io.nuvalence.cx.tools.cxtest.model.ResultLabel
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import kotlin.AssertionError

@Execution(ExecutionMode.CONCURRENT)
@Tag("dfcx")
@ExtendWith(DFCXTestBuilderExtension::class)
class DFCXTestBuilderSpec {
    companion object {
        val formattedResult: ThreadLocal<DFCXTestBuilderResult> = ThreadLocal()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DFCXTestBuilderExtension::class)
    fun testCases(displayName: String, testCase: TestCase, testCaseResult: TestCaseResult) {
        val testBuilderResult = DFCXTestBuilderResult(
            testCaseId = testCase.name,
            testCaseName = testCase.displayName,
            tags = testCase.tagsList.map { tag -> tag.toString() },
            notes = testCase.notes
        )
        formattedResult.set(testBuilderResult)

        val fullResult = DFCXTestBuilderExtension.testClient.getTestCaseResult(testCaseResult.name)

        fullResult.conversationTurnsList.forEachIndexed { index, turn ->
            val resultStep = DFCXTestBuilderResultStep(
                userInput = turn.userInput.input.text.text,
                expectedAgentOutput = testCase.testCaseConversationTurnsList[index].virtualAgentOutput.textResponsesList.joinToString("\n") { responseMessage ->
                    responseMessage.textList.reduce { acc, text -> acc + text }
                },
                actualAgentOutput = turn.virtualAgentOutput.textResponsesList.joinToString("\n") { responseMessage ->
                    responseMessage.textList.reduce { acc, text -> acc + text }
                },
                expectedPage = testCase.testCaseConversationTurnsList[index].virtualAgentOutput.currentPage.displayName,
                actualPage = turn.virtualAgentOutput.currentPage.displayName
            )

            val isStepWarn = turn.virtualAgentOutput.differencesList.any { diff ->
                diff.type == TestRunDifference.DiffType.UTTERANCE
            }

            val isStepError = turn.virtualAgentOutput.differencesList.any { diff ->
                diff.type == TestRunDifference.DiffType.PAGE
            }

            if (isStepError) {
                resultStep.result = ResultLabel.FAIL
            } else if (isStepWarn) {
                resultStep.result = ResultLabel.WARN
            }

            testBuilderResult.resultSteps.add(resultStep)
        }

        val isTestWarn = testBuilderResult.resultSteps.any { resultStep ->
            resultStep.result == ResultLabel.WARN
        }

        val isTestError = testBuilderResult.resultSteps.any { resultStep ->
            resultStep.result == ResultLabel.FAIL
        }

        if (isTestError) {
            testBuilderResult.result = ResultLabel.FAIL
        } else if (isTestWarn) {
            testBuilderResult.result = ResultLabel.WARN
        }


        if (testCaseResult.testResult == TestResult.FAILED) {
            testBuilderResult.result = ResultLabel.FAIL
            Assertions.fail<AssertionError>("Test failed")
        }
    }
}