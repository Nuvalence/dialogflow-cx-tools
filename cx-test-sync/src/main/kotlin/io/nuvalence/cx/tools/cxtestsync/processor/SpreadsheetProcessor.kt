package io.nuvalence.cx.tools.cxtestsync.processor

import com.google.api.services.sheets.v4.model.*
import gov.ny.dol.ui.ccai.dfcx.domain.model.artifact.DFCXTestBuilderResultArtifactFormat as Model
import io.nuvalence.cx.tools.cxtestcore.Properties
import io.nuvalence.cx.tools.cxtestsync.model.diff.DFCXTestDiff
import io.nuvalence.cx.tools.cxtestsync.model.test.DFCXInjectableTest
import io.nuvalence.cx.tools.cxtestsync.source.artifact.DFCXSpreadsheetArtifactSource
import io.nuvalence.cx.tools.cxtestsync.source.test.DFCXTestBuilderTestSource
import io.nuvalence.cx.tools.shared.SheetCopier
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class SpreadsheetProcessor {
    companion object {
        val url = Properties.getProperty<URL>("credentialsUrl")
        val spreadsheetId = Properties.getProperty<String>("spreadsheetId")
        lateinit var cols: Map<String, Int>
    }

    private fun createArtifact(destinationTitle: String) : String {
        val destinationSpreadsheetId = SheetCopier(url, spreadsheetId).copySpreadsheet(destinationTitle)
        deleteFirstSheet(destinationSpreadsheetId)
        clearResults(destinationSpreadsheetId)
        return destinationSpreadsheetId
    }

    private fun clearResults(spreadsheetId: String) {
        cols = DFCXSpreadsheetArtifactSource.cols
        SheetWriter(url, spreadsheetId).deleteCellRange("${'A' + cols[Model.TEST_RESULT.headerName]!!}2:${'A' + cols[Model.TEST_RESULT_DETAILS.headerName]!!}")
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


    private fun getTestDiff(sourceTest: DFCXInjectableTest, destinationTest: DFCXInjectableTest): DFCXTestDiff? {
        val testCaseNameDiff = sourceTest.testCaseName != destinationTest.testCaseName
        val tagsDiff = sourceTest.tags != destinationTest.tags
        val notesDiff = sourceTest.notes != destinationTest.notes
        val ssnDiff = sourceTest.ssn.isNotEmpty()

        return if (testCaseNameDiff || tagsDiff || notesDiff || ssnDiff) {
            DFCXTestDiff(
                testCaseId = destinationTest.testCaseId,
                testCaseName = if (testCaseNameDiff) { sourceTest.testCaseName } else null,
                tags = if (tagsDiff) { sourceTest.tags } else null,
                notes = if (notesDiff) { sourceTest.notes } else null,
                ssn = if (ssnDiff) { sourceTest.ssn } else null
            )
        } else null
    }

    fun process () {
        val artifactSource = DFCXSpreadsheetArtifactSource()
        val testSource = DFCXTestBuilderTestSource()

        val destinationSpreadsheetId = createArtifact("DFCX Synced Test Spreadsheet ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                Date()
            )}")

        val spreadsheetTests = artifactSource.getTestScenarios().sortedBy { it.testCaseId }
        val agentTests = testSource.getTestScenarios().sortedBy { it.testCaseId }

        val diffs = spreadsheetTests.fold(mutableListOf<DFCXTestDiff>()) { acc, spreadsheetTest ->
            val agentTest = agentTests.find { it.testCaseId == spreadsheetTest.testCaseId }
            if (agentTest != null) {
                val testDiff = getTestDiff(spreadsheetTest, agentTest)
                if (testDiff != null) acc.add(testDiff)
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

        if (diffs.isNotEmpty()) {
            println("Diffs:\n${diffs.joinToString("\n")}")
        }

        testSource.applyDiffs(diffs)

        println("Clean spreadsheet created, available at https://docs.google.com/spreadsheets/d/$destinationSpreadsheetId")
    }
}
