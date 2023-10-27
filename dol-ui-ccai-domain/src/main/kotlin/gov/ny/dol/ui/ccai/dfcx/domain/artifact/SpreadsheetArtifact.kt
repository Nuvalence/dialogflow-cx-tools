package gov.ny.dol.ui.ccai.dfcx.domain.artifact

import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest
import io.nuvalence.cx.tools.cxtestcore.Properties
import io.nuvalence.cx.tools.shared.SheetCopier
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.CellContentUpdateRequest
import java.net.URL

class SpreadsheetArtifact {
    companion object {
        val url = Properties.getProperty<URL>("credentialsUrl")
        val spreadsheetId = Properties.getProperty<String>("spreadsheetId")
        val agentPath = Properties.getProperty<String>("agentPath")
    }

    fun createArtifact(destinationTitle: String) : String {
        val destinationSpreadsheetId = SheetCopier(url, spreadsheetId).copySpreadsheet(destinationTitle)
        updateInfoSheet(destinationSpreadsheetId)
        return destinationSpreadsheetId
    }

    fun writeArtifact(spreadsheetId: String, requestData: Map<String, String>) {
        val cellContentUpdateRequests = requestData.map { (k, v) ->
            CellContentUpdateRequest(k, v)
        }
        return SheetWriter(url, spreadsheetId).batchUpdateCellContents(cellContentUpdateRequests)
    }

    private fun updateInfoSheet(destinationSpreadsheetId: String) {
        val title = "Test info"
        val firstSheetId = SheetReader(url, destinationSpreadsheetId, "").getSheets().firstOrNull()?.properties?.sheetId
        val sheetWriter = SheetWriter(url, destinationSpreadsheetId)
        sheetWriter.batchUpdateSheets(listOf(
            Request().setUpdateSheetProperties(
                UpdateSheetPropertiesRequest().setProperties(SheetProperties().setSheetId(firstSheetId).setTitle(title)).setFields("title")
            )))

        val updateCellRequests = listOf(
            CellContentUpdateRequest("${title}!A1", "Agent path"),
            CellContentUpdateRequest("${title}!B1", agentPath)
        )
        sheetWriter.batchUpdateCellContents(updateCellRequests)
    }
}