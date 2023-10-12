package io.nuvalence.cx.tools.cxtestsync.processor

import com.google.api.services.sheets.v4.model.*
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
    }

    private fun createArtifact(destinationTitle: String) : String {
        val destinationSpreadsheetId = SheetCopier(url, spreadsheetId).copySpreadsheet(destinationTitle)
        deleteFirstSheet(destinationSpreadsheetId)
        clearResults(destinationSpreadsheetId)
        return destinationSpreadsheetId
    }

    private fun clearResults(spreadsheetId: String) {
        return SheetWriter(url, spreadsheetId).deleteCellRange("F2:G")
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

    fun process () {
        val destinationSpreadsheetId = createArtifact("DFCX Synced Test Spreadsheet ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                Date()
            )}")

        println(destinationSpreadsheetId)

        // Update target agent
        // Test name, tags, and notes
    }
}
