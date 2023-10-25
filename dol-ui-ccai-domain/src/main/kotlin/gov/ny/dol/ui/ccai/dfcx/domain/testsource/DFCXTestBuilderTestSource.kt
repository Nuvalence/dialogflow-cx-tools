package gov.ny.dol.ui.ccai.dfcx.domain.testsource

import com.google.cloud.dialogflow.cx.v3.ListTestCasesRequest
import com.google.cloud.dialogflow.cx.v3.TestCase
import gov.ny.dol.ui.ccai.dfcx.domain.extension.DFCXTestBuilderExtension
import gov.ny.dol.ui.ccai.dfcx.domain.util.Properties
import java.util.*

class DFCXTestBuilderTestSource {
    fun getTestScenarios(): List<TestCase> {
        val listTestCasesRequest = ListTestCasesRequest.newBuilder()
            .setParent(Properties.AGENT_PATH)
            .setView(ListTestCasesRequest.TestCaseView.FULL)
            .setPageSize(20)
            .build()

        val testCasesResponse = DFCXTestBuilderExtension.testClient.listTestCases(listTestCasesRequest)
        val testCaseList = Collections.synchronizedList(mutableListOf<TestCase>())

        testCasesResponse.iteratePages().forEach { page ->
            testCaseList.addAll(page.response.testCasesList)
        }
        testCaseList.sortBy { testCase -> testCase.name }

        val tagFilter = Properties.DFCX_TAG_FILTER

        if (tagFilter != "ALL") {
            val tagFilters = tagFilter.split(',')

            val filteredTestCaseList = testCaseList.filter { testCase ->
                testCase.tagsList.containsAll(tagFilters)
            }

            println("Found ${filteredTestCaseList.size} tests")
            return filteredTestCaseList
        }

        println("Found ${testCaseList.size} tests")
        return testCaseList
    }
}