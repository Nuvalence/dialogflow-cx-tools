package io.nuvalence.cx.tools.cxtestsync.source.test

import com.google.cloud.dialogflow.cx.v3.*
import com.google.protobuf.FieldMask
import io.nuvalence.cx.tools.cxtestsync.model.test.DFCXTest
import io.nuvalence.cx.tools.cxtestsync.model.diff.DFCXTestDiff
import io.nuvalence.cx.tools.cxtestsync.model.test.DFCXTestStep
import io.nuvalence.cx.tools.cxtestsync.util.Properties
import java.util.*
import kotlin.reflect.full.companionObjectInstance

class DFCXTestBuilderTestSource {
    companion object {
        val testClient: TestCasesClient = TestCasesClient.create(
            TestCasesSettings.newBuilder()
                .setEndpoint(Properties.DFCX_ENDPOINT)
                .build())

        val dfcxTestCases = Collections.synchronizedList(mutableListOf<TestCase>())
        val testScenarios = mutableListOf<DFCXTest>()
    }

    init {
        getTestScenarios()
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
        if (testScenarios.isNotEmpty()) {
            return testScenarios
        }

        val listTestCasesRequest = ListTestCasesRequest.newBuilder()
            .setParent(Properties.AGENT_PATH)
            .setView(ListTestCasesRequest.TestCaseView.FULL)
            .setPageSize(20)
            .build()

        val testCasesResponse = testClient.listTestCases(listTestCasesRequest)

        testCasesResponse.iteratePages().forEach { page ->
            dfcxTestCases.addAll(page.response.testCasesList)
        }

        testScenarios.addAll(convertTestScenarios(dfcxTestCases))
        return testScenarios
    }

    fun applyDiffs(diffs: List<DFCXTestDiff>) {
        if (dfcxTestCases.isEmpty()) {
            getTestScenarios()
        }

        diffs.forEach { diff ->
            val testCase = dfcxTestCases.find { dfcxTestCase -> dfcxTestCase.name == diff.testCaseId }!!
            val updatedTestCaseBuilder = testCase.toBuilder()!!
            updatedTestCaseBuilder.displayName = diff.testCaseName ?: testCase.displayName
            updatedTestCaseBuilder.clearTags()
            updatedTestCaseBuilder.addAllTags(diff.tags ?: testCase.tagsList)
            updatedTestCaseBuilder.notes = diff.notes ?: testCase.notes

            val updateRequest = UpdateTestCaseRequest.newBuilder()
                .setTestCase(updatedTestCaseBuilder.build())
                .setUpdateMask(FieldMask.newBuilder().addAllPaths(listOf("display_name", "tags", "notes")).build())
                .build()

            testClient.updateTestCase(updateRequest)
        }
    }
}
