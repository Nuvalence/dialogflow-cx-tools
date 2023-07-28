package io.nuvalence.cx.tools.cxtest

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

    @TestFactory
    fun testCases(): List<DynamicTest> {
        println("Matching mode: ${PROPERTIES.MATCHING_MODE.get()}")
        val agentPath = PROPERTIES.AGENT_PATH.get()
        val (_, projectId, _, location, _, agentId) = agentPath.split("/")
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
