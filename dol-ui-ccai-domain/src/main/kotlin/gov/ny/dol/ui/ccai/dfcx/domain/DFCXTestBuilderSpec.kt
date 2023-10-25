package gov.ny.dol.ui.ccai.dfcx.domain

import com.google.cloud.dialogflow.cx.v3.*
import gov.ny.dol.ui.ccai.dfcx.domain.extension.DFCXTestBuilderExtension
import gov.ny.dol.ui.ccai.dfcx.domain.model.test.DFCXTestBuilderResult
import gov.ny.dol.ui.ccai.dfcx.domain.model.test.DFCXTestBuilderResultStep
import gov.ny.dol.ui.ccai.dfcx.domain.model.test.ResultLabel
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

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
                actualPage = turn.virtualAgentOutput.currentPage.displayName,
                diffs = turn.virtualAgentOutput.differencesList
            )

            if (turn.virtualAgentOutput.differencesList.any { diff ->
                diff.type == TestRunDifference.DiffType.PAGE || diff.type == TestRunDifference.DiffType.UTTERANCE
            }) {
                resultStep.result = ResultLabel.FAIL
            }

            testBuilderResult.resultSteps.add(resultStep)
        }

        if(testBuilderResult.resultSteps.any { resultStep ->
            resultStep.result == ResultLabel.FAIL
        }) {
            testBuilderResult.result = ResultLabel.FAIL
        }

        if (testCaseResult.testResult == TestResult.FAILED) {
            testBuilderResult.result = ResultLabel.FAIL
            Assertions.fail<AssertionError>("Test failed")
        }
    }
}