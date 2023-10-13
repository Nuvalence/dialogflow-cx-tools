package io.nuvalence.cx.tools.cxtestsync.processor

import com.google.api.services.sheets.v4.model.*
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTest
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTestDiff
import io.nuvalence.cx.tools.cxtestsync.source.artifact.DFCXSpreadsheetArtifactSource
import io.nuvalence.cx.tools.cxtestsync.source.test.DFCXTestBuilderTestSource
import io.nuvalence.cx.tools.cxtestsync.util.Properties
import io.nuvalence.cx.tools.shared.SheetCopier
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.UpdateRequest
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class SpreadsheetProcessor () {
    companion object {
        val url = Properties.CREDENTIALS_URL
        val spreadsheetId = Properties.SPREADSHEET_ID
        val agentPath = Properties.AGENT_PATH

        private const val TEST_CASE_NAME = "Test Case Name"
        private const val TEST_CASE_ID = "Test Case ID"
        private const val TAGS = "Tags"
        private const val NOTES = "Notes"
        private const val USER_INPUT = "User Input"
        private const val AGENT_OUTPUT = "Agent Output"
        private const val STATUS = "Status"
        private const val ERROR_DETAILS = "Error Details"

        val colNames = listOf(TEST_CASE_NAME, TAGS, NOTES, USER_INPUT, AGENT_OUTPUT, STATUS, ERROR_DETAILS)
    }

    private fun createArtifact(destinationTitle: String) : String {
        val destinationSpreadsheetId = SheetCopier(url, spreadsheetId).copySpreadsheet(destinationTitle)
        deleteFirstSheet(destinationSpreadsheetId)
        clearResults(destinationSpreadsheetId)
        return destinationSpreadsheetId
    }

    private fun clearResults(spreadsheetId: String) {
        SheetWriter(url, spreadsheetId).deleteCellRange("F2:G")
    }

    private fun deleteFirstSheet(destinationSpreadsheetId: String) {
        val firstSheetId = SheetReader(url, destinationSpreadsheetId, "").getSheets().firstOrNull()?.properties?.sheetId
        val sheetWriter = SheetWriter(url, destinationSpreadsheetId)

        if (firstSheetId != null) {
            val deleteRequest = Request().setDeleteSheet(
                DeleteSheetRequest().setSheetId(firstSheetId)
            )

            sheetWriter.batchUpdateSheets(listOf(deleteRequest))
        }
    }


    private fun getTestDiff(sourceTest: DFCXTest, destinationTest: DFCXTest): DFCXTestDiff? {
        val testCaseNameDiff = sourceTest.testCaseName != destinationTest.testCaseName
        val tagsDiff = sourceTest.tags != destinationTest.tags
        val notesDiff = sourceTest.notes != destinationTest.notes

        return if (testCaseNameDiff || tagsDiff || notesDiff) {
            DFCXTestDiff(
                testCaseId = destinationTest.testCaseId,
                testCaseName = if (testCaseNameDiff) { sourceTest.testCaseName } else null,
                tags = if (tagsDiff) { sourceTest.tags } else null,
                notes = if (notesDiff) { sourceTest.notes } else null
            )
        } else null
    }

    fun process () {
        val destinationSpreadsheetId = createArtifact("DFCX Synced Test Spreadsheet ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                Date()
            )}")

        val spreadsheetTests = DFCXSpreadsheetArtifactSource().getTestScenarios().sortedBy { it.testCaseId }
        val agentTests = DFCXTestBuilderTestSource().getTestScenarios().sortedBy { it.testCaseId }

        spreadsheetTests.fold(mutableListOf<DFCXTestDiff>()) { acc, spreadsheetTest ->
            val agentTest = agentTests.find { it.testCaseId == spreadsheetTest.testCaseId }!!
            val testDiff = getTestDiff(spreadsheetTest, agentTest)
            if (testDiff != null) acc.add(testDiff)
            acc
        }

        // update agent
    }
}
