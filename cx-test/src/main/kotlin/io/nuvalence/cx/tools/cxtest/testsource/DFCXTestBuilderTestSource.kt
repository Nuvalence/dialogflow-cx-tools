package io.nuvalence.cx.tools.cxtest.testsource

import com.google.cloud.dialogflow.cx.v3.ListTestCasesRequest
import com.google.cloud.dialogflow.cx.v3.TestCase
import io.nuvalence.cx.tools.cxtest.artifact.DFCXSpreadsheetArtifact
import io.nuvalence.cx.tools.cxtest.extension.DFCXTestBuilderExtension
import io.nuvalence.cx.tools.cxtest.util.Properties
import java.util.*

class DFCXTestBuilderTestSource {
    /**
     * Get all test scenarios from the agent. Tests are filtered by the DFCX_TAG_FILTER environment variable.
     *
     * @return List of test scenarios
     */
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

            DFCXSpreadsheetArtifact.summaryInfo.tagsIncluded = tagFilters.joinToString(", ")
            DFCXSpreadsheetArtifact.summaryInfo.tagsExcluded = tagExclusions.joinToString(", ")

            val filteredTestCaseList = testCaseList.filter { testCase ->
                tagFilters.isEmpty() || testCase.tagsList.containsAllIgnoreCase(tagFilters)
            }.filter { testCase ->
                !testCase.tagsList.any { tag -> tagExclusions.contains(tag) }
            }

            println("Found ${filteredTestCaseList.size} tests")
            return filteredTestCaseList
        }

        DFCXSpreadsheetArtifact.summaryInfo.tagsIncluded = "ALL"
        DFCXSpreadsheetArtifact.summaryInfo.tagsExcluded = ""

        println("Found ${testCaseList.size} tests")
        return testCaseList
    }

    private fun List<String>.containsAllIgnoreCase(comp: List<String>): Boolean {
        return comp.all { item ->
            this.any { it.equals(item, ignoreCase = true) }
        }
    }
}