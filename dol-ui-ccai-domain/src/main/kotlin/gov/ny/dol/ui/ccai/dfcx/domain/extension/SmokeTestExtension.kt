package gov.ny.dol.ui.ccai.dfcx.domain.extension

import com.google.cloud.dialogflow.cx.v3beta1.SessionsClient
import com.google.cloud.dialogflow.cx.v3beta1.SessionsSettings
import gov.ny.dol.ui.ccai.dfcx.domain.artifact.SpreadsheetArtifact
import gov.ny.dol.ui.ccai.dfcx.domain.assertion.ContextAwareAssertionError
import gov.ny.dol.ui.ccai.dfcx.domain.orchestrator.OrchestratedTestMap
import gov.ny.dol.ui.ccai.dfcx.domain.testsource.SmokeFormatReader
import gov.ny.dol.ui.ccai.dfcx.domain.util.Properties
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.api.extension.ExtensionContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.stream.Stream

class SmokeTestExtension : ArgumentsProvider, BeforeAllCallback, AfterAllCallback {
    companion object {
        val artifact = SpreadsheetArtifact()
        var sessionClient: SessionsClient? = null
    }

    override fun beforeAll(context: ExtensionContext?) {
        println("Agent: ${Properties.AGENT_PATH}")
        println("Matching mode: ${Properties.MATCHING_MODE}")

        val artifactSpreadsheetId = artifact.createArtifact("Smoke Spreadsheet ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                Date()
            )}")
        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("artifactSpreadsheetId", artifactSpreadsheetId)
        println("Created spreadsheet $artifactSpreadsheetId")

        sessionClient = SessionsClient.create(
            SessionsSettings.newBuilder()
                .setEndpoint(Properties.DFCX_ENDPOINT)
                .build())
    }

    override fun afterAll(context: ExtensionContext?) {
        val errorList = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.getOrComputeIfAbsent("errors") { mutableListOf<ContextAwareAssertionError>() } as MutableList<ContextAwareAssertionError>
        val artifactSpreadsheetId = context.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("artifactSpreadsheetId") as String

        val outputColumn = SmokeFormatReader.cols[SmokeFormatReader.COMMENTS]!!
        val resultColumn = SmokeFormatReader.cols[SmokeFormatReader.STEP_STATUS]!!

        val requestData = errorList.associate { e ->
            "${e.sourceId}!${'A' + outputColumn}${e.sourceLocator}" to e.message!!
        } + errorList.associate { e -> "${e.sourceId}!${'A' + resultColumn}${e.sourceLocator}" to "Fail" }

        artifact.writeArtifact(artifactSpreadsheetId, requestData)
        sessionClient?.close()
    }

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments?> {
        val testCases = SmokeFormatReader().listSheets("SMOKE_").map { sheet ->
            OrchestratedTestMap(SmokeFormatReader().read(sheet)).generatePairs()
        }.flatten()

        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("testCases", testCases)

        val testCaseArgs = testCases.map { (testScenario, executionPath) ->
             Arguments.of(testScenario, executionPath)
        }

        return Stream.of(*testCaseArgs.toTypedArray())
    }
}
