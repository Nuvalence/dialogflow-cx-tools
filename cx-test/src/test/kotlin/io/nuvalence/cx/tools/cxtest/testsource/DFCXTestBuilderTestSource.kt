package io.nuvalence.cx.tools.cxtest.testsource

import com.google.cloud.dialogflow.cx.v3.ListTestCasesRequest
import com.google.cloud.dialogflow.cx.v3.TestCase
import io.nuvalence.cx.tools.cxtest.extension.DFCXTestBuilderExtension
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import java.util.*

class DFCXTestBuilderTestSource {
    fun getTestScenarios(): MutableList<TestCase> {
        val listTestCasesRequest = ListTestCasesRequest.newBuilder()
            .setParent(PROPERTIES.AGENT_PATH.get())
            .setView(ListTestCasesRequest.TestCaseView.FULL)
            .setPageSize(20)
            .build()

        val testCasesResponse = DFCXTestBuilderExtension.testClient.listTestCases(listTestCasesRequest)
        val testCaseList = Collections.synchronizedList(mutableListOf<TestCase>())

        testCasesResponse.iteratePages().forEach { page ->
            testCaseList.addAll(page.response.testCasesList)
        }
        testCaseList.sortBy { testCase -> testCase.name }

        // TODO: filter on tags

        println("Found ${testCaseList.size} tests")
        return testCaseList
    }
}