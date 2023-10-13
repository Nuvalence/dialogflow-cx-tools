package io.nuvalence.cx.tools.cxtestsync.source.test

import com.google.cloud.dialogflow.cx.v3.ListTestCasesRequest
import com.google.cloud.dialogflow.cx.v3.TestCase
import com.google.cloud.dialogflow.cx.v3.TestCasesClient
import com.google.cloud.dialogflow.cx.v3.TestCasesSettings
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTest
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTestDiff
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTestStep
import io.nuvalence.cx.tools.cxtestsync.util.Properties
import java.util.*

class DFCXTestBuilderTestSource {
    companion object {
        val testClient: TestCasesClient = TestCasesClient.create(
            TestCasesSettings.newBuilder()
                .setEndpoint(Properties.DFCX_ENDPOINT)
                .build())
    }

    private fun convertTestScenarios(testCaseList: List<TestCase>): List<DFCXTest> {
        return testCaseList.map { testCase ->
            val test = DFCXTest(testCase.name, testCase.displayName, testCase.tagsList, testCase.notes)
            val testSteps = testCase.testCaseConversationTurnsList.map { turn ->
                DFCXTestStep(turn.userInput.input.text.text, turn.virtualAgentOutput.textResponsesList.joinToString("\n") { responseMessage ->
                    responseMessage.textList.reduce { acc, text -> acc + text }
                }, "", turn.virtualAgentOutput.currentPage.displayName, "")
            }
            test.resultSteps.addAll(testSteps)
            test
        }
    }

    fun getTestScenarios(): List<DFCXTest> {
        val listTestCasesRequest = ListTestCasesRequest.newBuilder()
            .setParent(Properties.AGENT_PATH)
            .setView(ListTestCasesRequest.TestCaseView.FULL)
            .setPageSize(20)
            .build()

        val testCasesResponse = testClient.listTestCases(listTestCasesRequest)
        val testCaseList = Collections.synchronizedList(mutableListOf<TestCase>())

        testCasesResponse.iteratePages().forEach { page ->
            testCaseList.addAll(page.response.testCasesList)
        }

        return convertTestScenarios(testCaseList)
    }

    fun applyDiffs(diffs: List<DFCXTestDiff>) {
        // TODO: apply diffs to tests in agent
    }
}
