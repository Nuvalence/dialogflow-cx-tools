package io.nuvalence.cx.tools.cxtest.extension

import com.google.cloud.dialogflow.cx.v3.*
import io.nuvalence.cx.tools.cxtest.DFCXTestBuilderSpec
import io.nuvalence.cx.tools.cxtest.artifact.DFCXSpreadsheetArtifact
import io.nuvalence.cx.tools.cxtest.assertion.ContextAwareAssertionError
import io.nuvalence.cx.tools.cxtest.model.DFCXTestBuilderResult
import io.nuvalence.cx.tools.cxtest.testsource.DFCXTestBuilderTestSource
import io.nuvalence.cx.tools.cxtest.testsource.SmokeFormatReader
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class DFCXTestBuilderExtension () : ArgumentsProvider, BeforeAllCallback, AfterAllCallback, AfterTestExecutionCallback {
    companion object {
        val artifact = DFCXSpreadsheetArtifact()
        lateinit var testClient: TestCasesClient
    }

    override fun beforeAll(context: ExtensionContext?) {
        println("Agent: ${PROPERTIES.AGENT_PATH.get()}")
        println("Creds URL: ${PROPERTIES.CREDENTIALS_URL.get()}")

        val artifactSpreadsheetId = artifact.createArtifact("DFCX Test Builder Spreadsheet ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                Date()
            )}")
        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("artifactSpreadsheetId", artifactSpreadsheetId)
        println("Created spreadsheet $artifactSpreadsheetId")

        testClient = TestCasesClient.create(
            TestCasesSettings.newBuilder()
                .setEndpoint(PROPERTIES.DFCX_ENDPOINT.get())
                .build())

        val testCaseList = DFCXTestBuilderTestSource().getTestScenarios()

        val request: BatchRunTestCasesRequest = BatchRunTestCasesRequest.newBuilder()
            .setParent(PROPERTIES.AGENT_PATH.get())
            .addAllTestCases(testCaseList.map { testCase -> testCase.name })
            .build()

        val response = testClient.batchRunTestCasesAsync(request).get()
        val resultsList = response.resultsList.sortedBy { result -> result.name }

        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("testCaseEntries", testCaseList zip resultsList)
        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("formattedResultList", Collections.synchronizedList(mutableListOf<DFCXTestBuilderResult>()))
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        val result = DFCXTestBuilderSpec.formattedResult.get()

        if (result != null) {
            println(result)
            val formattedResultList = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("formattedResultList") as MutableList<DFCXTestBuilderResult>
            formattedResultList.add(result)
            DFCXTestBuilderSpec.formattedResult.remove()
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        val artifactSpreadsheetId = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("artifactSpreadsheetId") as String
        val formattedResultList = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("formattedResultList") as MutableList<DFCXTestBuilderResult>
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
