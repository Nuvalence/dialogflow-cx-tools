package gov.ny.dol.ui.ccai.dfcx.domain.extension

import com.google.cloud.dialogflow.cx.v3.*
import gov.ny.dol.ui.ccai.dfcx.domain.test.spec.DFCXTestBuilderSpec
import gov.ny.dol.ui.ccai.dfcx.domain.artifact.DFCXSpreadsheetArtifact
import gov.ny.dol.ui.ccai.dfcx.domain.model.test.DFCXTest
import gov.ny.dol.ui.ccai.dfcx.domain.model.test.DFCXTestStep
import gov.ny.dol.ui.ccai.dfcx.domain.testsource.DFCXTestBuilderTestSource
import io.nuvalence.cx.tools.cxtestcore.Properties
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class DFCXTestBuilderExtension () : ArgumentsProvider, BeforeAllCallback, AfterAllCallback, AfterTestExecutionCallback {
    companion object {
        val artifact = DFCXSpreadsheetArtifact()
        lateinit var testClient: TestCasesClient
    }

    override fun beforeAll(context: ExtensionContext?) {
        println("Agent: ${Properties.getProperty<String>("agentPath")}")
        println("Creds URL: ${Properties.getProperty<URL>("credentialsUrl")}")

        val artifactSpreadsheetId = artifact.createArtifact("DFCX Test Builder Spreadsheet ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                Date()
            )}")
        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("artifactSpreadsheetId", artifactSpreadsheetId)
        println("Created spreadsheet https://docs.google.com/spreadsheets/d/$artifactSpreadsheetId")

        testClient = TestCasesClient.create(
            TestCasesSettings.newBuilder()
                .setEndpoint(Properties.getProperty<String>("dfcxEndpoint"))
                .build())

        val testCaseList = DFCXTestBuilderTestSource().getTestScenarios()

        if (testCaseList.isNotEmpty()) {
            val request: BatchRunTestCasesRequest = BatchRunTestCasesRequest.newBuilder()
                .setParent(Properties.getProperty<String>("agentPath"))
                .addAllTestCases(testCaseList.map { testCase -> testCase.name })
                .build()

            val response = testClient.batchRunTestCasesAsync(request).get()
            val resultsList = response.resultsList.sortedBy { result -> result.name }

            context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("testCaseEntries", testCaseList zip resultsList)
            context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("formattedResultList", Collections.synchronizedList(mutableListOf<DFCXTest<DFCXTestStep>>()))
        }
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        val result = DFCXTestBuilderSpec.formattedResult.get()

        if (result != null) {
            println(result)
            val formattedResultList = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("formattedResultList") as MutableList<DFCXTest<DFCXTestStep>>
            formattedResultList.add(result)
            DFCXTestBuilderSpec.formattedResult.remove()
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        val artifactSpreadsheetId = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("artifactSpreadsheetId") as String
        val formattedResultList = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("formattedResultList") as MutableList<DFCXTest<DFCXTestStep>>
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
