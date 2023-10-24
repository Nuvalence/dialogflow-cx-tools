package io.nuvalence.cx.tools.cxtest

import com.google.cloud.dialogflow.cx.v3beta1.*
import io.nuvalence.cx.tools.cxtest.orchestrator.OrchestratedTestMap
import io.nuvalence.cx.tools.cxtest.testsource.E2EFormatReader
import io.nuvalence.cx.tools.cxtest.util.Properties
import io.nuvalence.cx.tools.cxtest.assertion.assertFuzzyMatch
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.*


@Execution(ExecutionMode.CONCURRENT)
@Tag("e2e")
class E2ESpec {
    companion object {
        private const val EN_SHEET = "E2E_EN_TestCases"
        private const val ES_SHEET = "E2E_ES_TestCases"

        private const val EN_LANG = "en-US"
        private const val ES_LANG = "es-ES"

        val e2eSheets = mapOf(
            EN_SHEET to EN_LANG, ES_SHEET to ES_LANG
        )
    }

    private val sessionClient: SessionsClient = SessionsClient.create(
        SessionsSettings.newBuilder()
        .setEndpoint(Properties.DFCX_ENDPOINT)
        .build()
    )

    @TestFactory
    fun testCases(): List<DynamicTest> {
        println("Matching mode: ${Properties.MATCHING_MODE}")
        val agentPath = Properties.AGENT_PATH
        val (_, projectId, _, location, _, agentId) = agentPath.split("/")
        return e2eSheets.map { sheet ->
            OrchestratedTestMap(E2EFormatReader().read(sheet.key, sheet.value)).generatePairs()
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
