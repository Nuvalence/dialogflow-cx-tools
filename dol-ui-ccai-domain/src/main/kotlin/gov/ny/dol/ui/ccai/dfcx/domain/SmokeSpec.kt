package gov.ny.dol.ui.ccai.dfcx.domain

import com.google.cloud.dialogflow.cx.v3beta1.*
import gov.ny.dol.ui.ccai.dfcx.domain.assertion.ContextAwareAssertionError
import gov.ny.dol.ui.ccai.dfcx.domain.assertion.assertFuzzyMatch
import gov.ny.dol.ui.ccai.dfcx.domain.extension.SmokeTestExtension
import gov.ny.dol.ui.ccai.dfcx.domain.listener.DynamicTestListener
import gov.ny.dol.ui.ccai.dfcx.domain.model.test.TestScenario
import gov.ny.dol.ui.ccai.dfcx.domain.orchestrator.ExecutionPath
import gov.ny.dol.ui.ccai.dfcx.domain.util.Properties
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.*


@Execution(ExecutionMode.CONCURRENT)
@Tag("smoke")
@ExtendWith(DynamicTestListener::class, SmokeTestExtension::class)
class SmokeSpec {
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SmokeTestExtension::class)
    fun testCases(testScenario: TestScenario, executionPath: ExecutionPath) {
        val agentPath = Properties.AGENT_PATH
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
            val detectIntentResponse = SmokeTestExtension.sessionClient?.detectIntent(detectIntentRequest)
            val response = detectIntentResponse?.queryResult?.responseMessagesList
            try {
                if (response != null) {
                    assertFuzzyMatch(currentPathInput, expectedResponse, response)
                }
            } catch (e: AssertionError) {
                throw ContextAwareAssertionError(e.message, testScenario.sourceId, sourceLocator)
            }
        }
    }
}

private operator fun <E> List<E>.component6(): E {
    return get(5)
}
