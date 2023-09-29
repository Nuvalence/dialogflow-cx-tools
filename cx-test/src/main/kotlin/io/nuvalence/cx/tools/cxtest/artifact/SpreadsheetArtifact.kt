package io.nuvalence.cx.tools.cxtest.artifact

import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.shared.SheetCopier
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.UpdateRequest
import java.net.URL

class SpreadsheetArtifact {
    companion object {
        val url = PROPERTIES.CREDENTIALS_URL.get()!!
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()!!
        val agentPath = PROPERTIES.AGENT_PATH.get()!!
    }

    fun createArtifact(destinationTitle: String) : String {
        val destinationSpreadsheetId = SheetCopier(URL(url), spreadsheetId).copySpreadsheet(destinationTitle)
        updateInfoSheet(destinationSpreadsheetId)
        return destinationSpreadsheetId
    }

    fun writeArtifact(spreadsheetId: String, requestData: Map<String, String>) {
        val updateRequests = requestData.map { (k, v) ->
            UpdateRequest(k, v)
        }
        return SheetWriter(URL(url), spreadsheetId).batchUpdateCells(updateRequests)
    }

    private fun updateInfoSheet(destinationSpreadsheetId: String) {
        val title = "Test info"
        val firstSheetId = SheetReader(URL(url), destinationSpreadsheetId, "").getSheets().firstOrNull()?.properties?.sheetId
        val sheetWriter = SheetWriter(URL(url), destinationSpreadsheetId)
        sheetWriter.batchUpdateSheets(listOf(
            Request().setUpdateSheetProperties(
                UpdateSheetPropertiesRequest().setProperties(SheetProperties().setSheetId(firstSheetId).setTitle(title)).setFields("title")
            )))

        val updateCellRequests = listOf(
            UpdateRequest("${title}!A1", "Agent path"),
            UpdateRequest("${title}!B1", agentPath)
        )
        sheetWriter.batchUpdateCells(updateCellRequests)
    }
}