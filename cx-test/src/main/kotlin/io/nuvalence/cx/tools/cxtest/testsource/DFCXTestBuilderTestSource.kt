package io.nuvalence.cx.tools.cxtest.testsource

import com.google.cloud.dialogflow.cx.v3.ListTestCasesRequest
import com.google.cloud.dialogflow.cx.v3.TestCase
import io.nuvalence.cx.tools.cxtest.extension.DFCXTestBuilderExtension
import io.nuvalence.cx.tools.cxtest.util.Properties
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
            val (tagExclusionsRaw, tagFilters) = tagFilter.split(',').partition { it.startsWith('!') }
            val tagExclusions = tagExclusionsRaw.map { it.substring(1) }

            println("Tag exclusions: $tagExclusions")
            println("Tag filters: $tagFilters")

            val filteredTestCaseList = testCaseList.filter { testCase ->
                testCase.tagsList.containsAll(tagFilters)
            }.filter { testCase ->
                !testCase.tagsList.any { tag -> tagExclusions.contains(tag) }
            }

            println("Found ${filteredTestCaseList.size} tests")
            return filteredTestCaseList
        }

        println("Found ${testCaseList.size} tests")
        return testCaseList
    }
}