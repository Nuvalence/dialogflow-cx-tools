package io.nuvalence.cx.tools.cxtest.artifact

import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest
import io.nuvalence.cx.tools.cxtest.model.DFCXTestBuilderResult
import io.nuvalence.cx.tools.cxtest.model.ResultLabel
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.shared.SheetCreator
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.UpdateRequest
import java.net.URL
import kotlin.properties.Delegates

class DFCXSpreadsheetArtifact {
    companion object {
        val url = PROPERTIES.CREDENTIALS_URL.get()!!

        const val sheetTitle = "Test Results"
        var sheetId by Delegates.notNull<Int>()

        private const val TEST_CASE_NAME = "Test Case Name"
        private const val TAGS = "Tags"
        private const val NOTES = "Notes"
        private const val USER_INPUT = "User Input"
        private const val AGENT_OUTPUT = "Agent Output"
        private const val STATUS = "Status"
        private const val ERROR_DETAILS = "Error Details"

        val colNames = listOf(TEST_CASE_NAME, TAGS, NOTES, USER_INPUT, AGENT_OUTPUT, STATUS, ERROR_DETAILS)
    }

    fun createArtifact(title: String) : String {
        val spreadsheetId = SheetCreator(URL(url)).createNewSpreadsheet(title)
        sheetId = SheetReader(URL(url), spreadsheetId, "").getSheets().firstOrNull()?.properties?.sheetId!!
        updateResultsSheet(spreadsheetId)
        return spreadsheetId
    }

    fun writeArtifact(spreadsheetId: String, formattedResultsList: List<DFCXTestBuilderResult>) {
        val sheetWriter = SheetWriter(URL(url), spreadsheetId)

        // Gather total rows
        val totalRowCount = formattedResultsList.fold(0) { acc, result -> acc + result.resultSteps.size }
        sheetWriter.addEmptyRows(totalRowCount+1, spreadsheetId, sheetTitle, sheetId)

        // Header row
        val requestData = colNames.withIndex().associate { e ->
            "${sheetTitle}!${'A' + e.index}1" to e.value
        }.toMutableMap()


        var rowCounter = 2;
        // For each test
        formattedResultsList.forEach { result ->
            // Add display name, tags, notes
            requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(TEST_CASE_NAME)}${rowCounter}", result.testCaseName)
            requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(TAGS)}${rowCounter}", result.tags.toString())
            requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(NOTES)}${rowCounter}", result.notes)

            // For each step
            // Add user input, agent output, status, error details
            result.resultSteps.forEach { resultStep ->
                requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(USER_INPUT)}${rowCounter}", resultStep.userInput)
                requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(AGENT_OUTPUT)}${rowCounter}", resultStep.actualAgentOutput)
                requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(STATUS)}${rowCounter}", resultStep.result.value)

                if (resultStep.result == ResultLabel.WARN) {
                    val message = "Mismatch with expected agent output: ${resultStep.expectedAgentOutput}"
                    requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(ERROR_DETAILS)}${rowCounter}", message)
                } else if (resultStep.result == ResultLabel.FAIL) {
                    val message = "Mismatch with expected agent output: ${resultStep.expectedAgentOutput}\n| Expected to be on page ${resultStep.expectedPage} but ended on ${resultStep.actualPage}"
                    requestData += Pair("${sheetTitle}!${'A' + colNames.indexOf(ERROR_DETAILS)}${rowCounter}", message)
                }

                rowCounter++
            }
        }


        val updateRequests = requestData.map { (k, v) ->
            UpdateRequest(k, v)
        }
        return sheetWriter.batchUpdateCells(updateRequests)
    }

    private fun updateResultsSheet(destinationSpreadsheetId: String) {
        val sheetWriter = SheetWriter(URL(url), destinationSpreadsheetId)
        sheetWriter.batchUpdateSheets(listOf(
            Request().setUpdateSheetProperties(
                UpdateSheetPropertiesRequest().setProperties(SheetProperties().setSheetId(sheetId).setTitle(
                    sheetTitle)).setFields("title")
            )))
    }

}