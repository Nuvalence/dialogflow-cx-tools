package io.nuvalence.cx.tools.cxtestsync.processor

import com.google.api.services.sheets.v4.model.*
import io.nuvalence.cx.tools.cxtestsync.model.artifact.DFCXTestSpreadsheetModel
import io.nuvalence.cx.tools.cxtestsync.model.test.DFCXTest
import io.nuvalence.cx.tools.cxtestsync.model.diff.DFCXTestDiff
import io.nuvalence.cx.tools.cxtestsync.source.artifact.DFCXSpreadsheetArtifactSource
import io.nuvalence.cx.tools.cxtestsync.source.test.DFCXTestBuilderTestSource
import io.nuvalence.cx.tools.cxtestsync.util.Properties
import io.nuvalence.cx.tools.shared.SheetCopier
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import java.text.SimpleDateFormat
import java.util.*

class SpreadsheetProcessor () {
    companion object {
        val url = Properties.CREDENTIALS_URL
        val spreadsheetId = Properties.SPREADSHEET_ID
    }

    private fun createArtifact(destinationTitle: String) : String {
        val destinationSpreadsheetId = SheetCopier(url, spreadsheetId).copySpreadsheet(destinationTitle)
        deleteFirstSheet(destinationSpreadsheetId)
        clearResults(destinationSpreadsheetId)
        return destinationSpreadsheetId
    }

    private fun clearResults(spreadsheetId: String) {
        SheetWriter(url, spreadsheetId).deleteCellRange("${'A' + DFCXTestSpreadsheetModel.colNames.indexOf(DFCXTestSpreadsheetModel.TEST_RESULT)}2:${'A' + DFCXTestSpreadsheetModel.colNames.indexOf(DFCXTestSpreadsheetModel.TEST_RESULT_DETAILS)}")
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

        val artifactSource = DFCXSpreadsheetArtifactSource()
        val testSource = DFCXTestBuilderTestSource()

        val spreadsheetTests = artifactSource.getTestScenarios().sortedBy { it.testCaseId }
        val agentTests = testSource.getTestScenarios().sortedBy { it.testCaseId }

        val diffs = spreadsheetTests.fold(mutableListOf<DFCXTestDiff>()) { acc, spreadsheetTest ->
            val agentTest = agentTests.find { it.testCaseId == spreadsheetTest.testCaseId }
            if (agentTest != null) {
                val testDiff = getTestDiff(spreadsheetTest, agentTest)
                if (testDiff !== null) acc.add(testDiff)
            }
            acc
        }

        val spreadsheetTestIds = spreadsheetTests.map {it.testCaseId}
        val agentTestIds = agentTests.map {it.testCaseId}

        val spreadsheetExclusives = spreadsheetTestIds.minus(agentTestIds.toSet())
        if (spreadsheetExclusives.isNotEmpty()) {
            println("The following tests are missing from the agent:\n${spreadsheetExclusives.joinToString("\n")}")
        }

        val agentExclusives = agentTestIds.minus(spreadsheetTestIds.toSet())
        if (agentExclusives.isNotEmpty()) {
            println("The following tests are missing from the spreadsheet:\n${agentExclusives.joinToString("\n")}")
        }

        println("Diffs:\n${diffs.joinToString("\n")}")

        testSource.applyDiffs(diffs)

        println("Clean spreadsheet created, ID: $destinationSpreadsheetId")
    }
}
