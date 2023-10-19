package io.nuvalence.cx.tools.cxtestsync.source.test

import com.google.cloud.dialogflow.cx.v3.*
import com.google.protobuf.FieldMask
import io.nuvalence.cx.tools.cxtestsync.model.diff.DFCXTestDiff
import io.nuvalence.cx.tools.cxtestsync.model.test.DFCXInjectableTest
import io.nuvalence.cx.tools.cxtestsync.model.test.DFCXInjectableTestStep
import io.nuvalence.cx.tools.cxtestsync.util.Properties
import java.util.*

class DFCXTestBuilderTestSource {
    companion object {
        val testClient: TestCasesClient = TestCasesClient.create(
            TestCasesSettings.newBuilder()
                .setEndpoint(Properties.DFCX_ENDPOINT)
                .build())

        val dfcxTestCases = Collections.synchronizedList(mutableListOf<TestCase>())
        val testScenarios = mutableListOf<DFCXInjectableTest>()
    }

    init {
        getTestScenarios()
    }

    private fun convertTestScenarios(testCaseList: List<TestCase>): List<DFCXInjectableTest> {
        return testCaseList.map { testCase ->
            val test = DFCXInjectableTest(testCase.name, testCase.displayName, testCase.tagsList, testCase.notes, "")
            val testSteps = testCase.testCaseConversationTurnsList.map { turn ->
                DFCXInjectableTestStep(turn.userInput.input.text.text, turn.virtualAgentOutput.textResponsesList.joinToString("\n") { responseMessage ->
                    responseMessage.textList.reduce { acc, text -> acc + text }
                }, "", turn.virtualAgentOutput.currentPage.displayName, "", mapOf("" to ""))
            }
            test.resultSteps.addAll(testSteps)
            test
        }
    }

    fun getTestScenarios(): List<DFCXInjectableTest> {
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

    private fun updateSsn(testCase: TestCase.Builder, diff: DFCXTestDiff) {
        if (diff.ssn != null) {
            testCase.testCaseConversationTurnsList?.forEachIndexed { index, turn ->
                if (index > 0 && testCase.testCaseConversationTurnsList!![index - 1].virtualAgentOutput.sessionParameters.fieldsMap["data-collection-type"]?.stringValue == "ssn" &&
                    turn.userInput.input.text.text.matches(Regex(".*\\d{9}.*"))) {
                    val updatedTurn = turn.toBuilder()
                        .setUserInput(
                            turn.userInput?.toBuilder()!!.setInput(
                                QueryInput.newBuilder().setText(
                                    TextInput.newBuilder().setText(diff.ssn).build()
                                )
                            ).build()
                        )
                        /*
                        .setVirtualAgentOutput(originalStep.virtualAgentOutput?.toBuilder()!!.setSessionParameters(
                            originalStep.virtualAgentOutput.sessionParameters.toBuilder()!!.fieldsMap["ssn"] = "987654321"
                        ))
                        */
                        .build()

                    testCase.setTestCaseConversationTurns(index, updatedTurn)
                }
            }
        }
    }

    private fun getFieldMasksFromDiffs(diffs: List<DFCXTestDiff>): List<String> {
        val list = buildList {
            if (diffs.find{ diff -> diff.testCaseName != null } != null) add("display_name")
            if (diffs.find{ diff -> diff.tags != null } != null) add("tags")
            if (diffs.find{ diff -> diff.notes != null } != null) add("notes")
            if (diffs.find{ diff -> diff.ssn != null } != null) add("test_case_conversation_turns")
        }

        return list
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
            updateSsn(updatedTestCaseBuilder, diff)

            val updateRequest = UpdateTestCaseRequest.newBuilder()
                .setTestCase(updatedTestCaseBuilder.build())
                .setUpdateMask(FieldMask.newBuilder().addAllPaths(getFieldMasksFromDiffs(diffs)).build())
                .build()

            testClient.updateTestCase(updateRequest)
        }
    }
}
