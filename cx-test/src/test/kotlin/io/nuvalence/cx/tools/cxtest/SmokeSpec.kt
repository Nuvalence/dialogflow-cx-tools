package io.nuvalence.cx.tools.cxtest

import com.google.cloud.dialogflow.cx.v3beta1.*
import io.nuvalence.cx.tools.cxtest.assertion.ContextAwareAssertionError
import io.nuvalence.cx.tools.cxtest.extension.SmokeTestExtension
import io.nuvalence.cx.tools.cxtest.listener.DynamicTestListener
import io.nuvalence.cx.tools.cxtest.model.TestScenario
import io.nuvalence.cx.tools.cxtest.orchestrator.ExecutionPath
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.cxtest.assertion.assertFuzzyMatch
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.IOException
import java.util.UUID


@Execution(ExecutionMode.CONCURRENT)
@Tag("smoke")
@ExtendWith(DynamicTestListener::class, SmokeTestExtension::class)
class SmokeSpec {
    private val sessionClient: SessionsClient = SessionsClient.create(
        SessionsSettings.newBuilder()
            .setEndpoint(PROPERTIES.DFCX_ENDPOINT.get())
            .build()
    )

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SmokeTestExtension::class)
    fun testCases(testScenario: TestScenario, executionPath: ExecutionPath) {
        val agentPath = PROPERTIES.AGENT_PATH.get()!!
        val (_, projectId, _, location, _, agentId) = agentPath.split("/")
        val sessionPath =
            SessionName.format(projectId, location, agentId, "test-session-" + UUID.randomUUID())
        testScenario.testSteps.forEachIndexed { index, (input, expectedResponse, sourceLocator) ->
            val currentPathInput = input[executionPath[index]]
            val queryInput = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(currentPathInput).build())
                .setLanguageCode(testScenario.languageCode).build()
            val detectIntentRequest =
                DetectIntentRequest.newBuilder().setSession(sessionPath.toString())
                    .setQueryInput(queryInput).build()
            val detectIntentResponse = sessionClient.detectIntent(detectIntentRequest)
            val response = detectIntentResponse.queryResult.responseMessagesList

            try {
                assertFuzzyMatch(currentPathInput, expectedResponse, response)
            } catch (e: AssertionError) {
                throw ContextAwareAssertionError(e.message, testScenario.sourceId, sourceLocator)
            }
        }
    }
}

private operator fun <E> List<E>.component6(): E {
    return get(5)
}
