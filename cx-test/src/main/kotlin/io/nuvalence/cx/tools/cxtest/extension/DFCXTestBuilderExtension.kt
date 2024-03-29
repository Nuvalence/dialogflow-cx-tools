package io.nuvalence.cx.tools.cxtest.extension

import com.google.cloud.dialogflow.cx.v3.*
import io.nuvalence.cx.tools.cxtest.DFCXTestBuilderSpec
import io.nuvalence.cx.tools.cxtest.artifact.DFCXSpreadsheetArtifact
import io.nuvalence.cx.tools.cxtest.model.test.DFCXTestBuilderResult
import io.nuvalence.cx.tools.cxtest.model.test.ResultLabel
import io.nuvalence.cx.tools.cxtest.testsource.DFCXTestBuilderTestSource
import io.nuvalence.cx.tools.cxtest.util.Properties
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class DFCXTestBuilderExtension : ArgumentsProvider, BeforeAllCallback, AfterAllCallback, AfterTestExecutionCallback {
    companion object {
        val artifact = DFCXSpreadsheetArtifact()
        lateinit var testClient: TestCasesClient
        lateinit var agentsClient : AgentsClient
    }

    override fun beforeAll(context: ExtensionContext?) {
        println("Agent: ${Properties.AGENT_PATH}")
        println("Creds URL: ${Properties.CREDENTIALS_URL}")

        val currentTimestamp = Date()
        val timestampString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentTimestamp)
        DFCXSpreadsheetArtifact.summaryInfo.testTimestamp = timestampString

        val artifactSpreadsheetId = artifact.createArtifact("DFCX Test Builder Spreadsheet $timestampString")
        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("artifactSpreadsheetId", artifactSpreadsheetId)
        println("Created spreadsheet https://docs.google.com/spreadsheets/d/$artifactSpreadsheetId")

        testClient = TestCasesClient.create(
            TestCasesSettings.newBuilder()
                .setEndpoint(Properties.DFCX_ENDPOINT)
                .build())
        agentsClient = AgentsClient.create(AgentsSettings.newBuilder()
            .setEndpoint(Properties.DFCX_ENDPOINT)
            .build())

        DFCXSpreadsheetArtifact.summaryInfo.agentName = agentsClient.getAgent(Properties.AGENT_PATH).displayName

        val testCaseList = DFCXTestBuilderTestSource().getTestScenarios()
        if (testCaseList.isEmpty()) {
            println("No test cases found")
            return
        }

        DFCXSpreadsheetArtifact.summaryInfo.testsRun = testCaseList.size
        DFCXSpreadsheetArtifact.summaryInfo.testsPassed = 0
        DFCXSpreadsheetArtifact.summaryInfo.testsFailed = 0

        DFCXTestBuilderSpec.formattedResults.set(mutableMapOf())

        try {
            val request: BatchRunTestCasesRequest = BatchRunTestCasesRequest.newBuilder()
                .setParent(Properties.AGENT_PATH)
                .addAllTestCases(testCaseList.map { testCase -> testCase.name })
                .build()

            val response = testClient.batchRunTestCasesAsync(request).get()
            val resultsList = response.resultsList.sortedBy { result -> result.name }

            context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)
                ?.put("testCaseEntries", testCaseList zip resultsList)
            context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)
                ?.put("formattedResultMap", Collections.synchronizedMap(mutableMapOf<String, DFCXTestBuilderResult>()))
        } catch (e: Exception) {
            println("Error running tests: ${e.message}")
        }
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        val formattedResultMap = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("formattedResultMap") as MutableMap<String, DFCXTestBuilderResult>
        val incomingTestResults = DFCXTestBuilderSpec.formattedResults.get()
        synchronized(incomingTestResults) {
            formattedResultMap.putAll(incomingTestResults)
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        val artifactSpreadsheetId = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("artifactSpreadsheetId") as String
        val formattedResultList = (context.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("formattedResultMap") as MutableMap<String, DFCXTestBuilderResult>)
            .values.toList()

        DFCXSpreadsheetArtifact.summaryInfo.testsPassed = formattedResultList.count { it.result == ResultLabel.PASS }
        DFCXSpreadsheetArtifact.summaryInfo.testsFailed = formattedResultList.count { it.result == ResultLabel.FAIL }

        try {
            val intentCoverage = testClient.calculateCoverage(CalculateCoverageRequest.newBuilder()
                .setAgent(Properties.AGENT_PATH)
                .setType(CalculateCoverageRequest.CoverageType.INTENT)
                .build())
            DFCXSpreadsheetArtifact.summaryInfo.intentsCoverage = "${(intentCoverage.intentCoverage.coverageScore * 100)}%"

            val transitionCoverage = testClient.calculateCoverage(CalculateCoverageRequest.newBuilder()
                .setAgent(Properties.AGENT_PATH)
                .setType(CalculateCoverageRequest.CoverageType.PAGE_TRANSITION)
                .build())
            DFCXSpreadsheetArtifact.summaryInfo.transitionsCoverage =
                "${(transitionCoverage.transitionCoverage.coverageScore * 100)}%"

            val routeGroupsCoverage = testClient.calculateCoverage(CalculateCoverageRequest.newBuilder()
                .setAgent(Properties.AGENT_PATH)
                .setType(CalculateCoverageRequest.CoverageType.TRANSITION_ROUTE_GROUP)
                .build())
            DFCXSpreadsheetArtifact.summaryInfo.routeGroupsCoverage = "${(routeGroupsCoverage.routeGroupCoverage.coverageScore * 100)}%"
        } catch (e: Exception) {
            println("Error calculating coverage: ${e.message}")
        }

        artifact.writeArtifact(artifactSpreadsheetId, formattedResultList)

        testClient.close()
    }

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments?> {
        val testCaseEntries = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("testCaseEntries") as List<Pair<TestCase, TestCaseResult>>

        val testCaseArgs = testCaseEntries.map { (testCase, testCaseResult) ->
            Arguments.of(testCase.displayName, testCase, testCaseResult)
        }

        return Stream.of(*testCaseArgs.toTypedArray())
    }
}
