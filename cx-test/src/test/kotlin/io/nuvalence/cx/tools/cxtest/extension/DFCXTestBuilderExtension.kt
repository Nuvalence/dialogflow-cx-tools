package io.nuvalence.cx.tools.cxtest.extension

import com.google.cloud.dialogflow.cx.v3.*
import io.nuvalence.cx.tools.cxtest.DFCXTestBuilderSpec
import io.nuvalence.cx.tools.cxtest.testsource.DFCXTestBuilderTestSource
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.*
import java.util.stream.Stream

class DFCXTestBuilderExtension () : ArgumentsProvider, BeforeAllCallback, AfterAllCallback, AfterTestExecutionCallback {
    companion object {
        lateinit var testClient: TestCasesClient
    }

    override fun beforeAll(context: ExtensionContext?) {
        println("Agent: ${PROPERTIES.AGENT_PATH.get()}")

        // TODO: Create artifact spreadsheet

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
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        val result = DFCXTestBuilderSpec.formattedResult.get()

        if (result != null) {
            println(result)
            DFCXTestBuilderSpec.formattedResult.remove()
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        // TODO: create
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
