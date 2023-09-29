package io.nuvalence.cx.tools.cxtest

import com.google.cloud.dialogflow.cx.v3.ConversationTurn
import com.google.cloud.dialogflow.cx.v3.TestCaseName
import com.google.cloud.dialogflow.cx.v3.TestCasesClient
import com.google.cloud.dialogflow.cx.v3.TestCasesSettings
import com.google.cloud.dialogflow.cx.v3beta1.*
import io.nuvalence.cx.tools.cxtest.orchestrator.OrchestratedTestMap
import io.nuvalence.cx.tools.cxtest.sheetformat.SmokeFormatReader
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.cxtest.util.assertFuzzyMatch
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.*


@Execution(ExecutionMode.CONCURRENT)
@Tag("smoke")
class SmokeSpec {
    private val sessionClient: SessionsClient = SessionsClient.create(
        SessionsSettings.newBuilder()
            .setEndpoint(PROPERTIES.DFCX_ENDPOINT.get())
            .build()
    )

    private val testClient: TestCasesClient = TestCasesClient.create(
        TestCasesSettings.newBuilder()
            .setEndpoint(PROPERTIES.DFCX_ENDPOINT.get())
            .build()
    )

    fun parseJson(data: List<ConversationTurn>) {
        for (i in data.indices) {
            val turn = data[i];

            // User Input
            if (turn.hasUserInput()) {
                val userInput = turn.userInput
                if (userInput.hasInput()) {
                    val input = userInput.input
                    if (input.hasText()) {
                        val text = input.text
                        println("CALLER SAYS: ${text.text}")
                    }
                }
            }

            // Virtual Agent Output
            if (turn.hasVirtualAgentOutput()) {
                val virtualAgentOutput = turn.virtualAgentOutput
                if (virtualAgentOutput.textResponsesCount > 0) {
                    val textResponses = virtualAgentOutput.textResponsesList
                    for (j in 0 until textResponses.size) {
                        val response = textResponses[j]
                        if (response.textCount > 0) {
                            println("AGENT SAYS: ${response.textList}")
                        }
                    }
                }
                if (virtualAgentOutput.differencesCount > 0) {
                    val differences = virtualAgentOutput.differencesList

                    println(differences.toString())
                }
            }
        }
    }

    @TestFactory
    fun testCases(): List<DynamicTest> {

        val testCase = testClient.getTestCase(
            TestCaseName.of(
                "dol-uisim-ccai-dev-app",
                "global",
                "375be0f6-4a92-4cd3-a4fc-a47226c748cc",
                "008f032b-d917-4101-ab7f-6ef9f6b20c59"
            ).toString()
        )

        testClient.updateTestCase()

        println(testCase.displayName)
        println(testCase.tagsList)
        println(testCase.notes)
        parseJson(testCase.lastTestResult.conversationTurnsList);
        println(testCase.lastTestResult)

        println(testCase.lastTestResult.conversationTurnsList);

        println("Matching mode: ${PROPERTIES.MATCHING_MODE.get()}")
        val agentPath = PROPERTIES.AGENT_PATH.get()
        val (_, projectId, _, location, _, agentId) = agentPath.split("/")

        SmokeFormatReader().listSheets("SMOKE_").map { sheet ->
            OrchestratedTestMap(SmokeFormatReader().read(sheet)).generatePairs()
                .map { (testScenario, executionPath) ->
                    DynamicTest.dynamicTest(testScenario.title) {
                        println(testScenario.title)
                    }
                }
        }

        return SmokeFormatReader().listSheets("SMOKE_").map { sheet ->
            OrchestratedTestMap(SmokeFormatReader().read(sheet)).generatePairs()
                .map { (testScenario, executionPath) ->
                    DynamicTest.dynamicTest(testScenario.title) {
                        val sessionPath =
                            SessionName.format(projectId, location, agentId, "test-session-" + UUID.randomUUID())
                        testScenario.testSteps.forEachIndexed { index, (input, expectedResponse) ->
                            val currentPathInput = input[executionPath[index]]
                            val queryInput = QueryInput.newBuilder()
                                .setText(TextInput.newBuilder().setText(currentPathInput).build())
                                .setLanguageCode(testScenario.languageCode).build()
                            val detectIntentRequest =
                                DetectIntentRequest.newBuilder().setSession(sessionPath.toString())
                                    .setQueryInput(queryInput).build()
                            val detectIntentResponse = sessionClient.detectIntent(detectIntentRequest)
                            val response = detectIntentResponse.queryResult.responseMessagesList

                            assertFuzzyMatch(currentPathInput, expectedResponse, response)
                        }
                    }
                }
        }.flatten()
    }
}

private operator fun <E> List<E>.component6(): E {
    return get(5)
}
