package io.nuvalence.cx.tools.cxtest.artifact

import io.nuvalence.cx.tools.cxtest.assertion.ContextAwareAssertionError
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.shared.SheetCopier
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.UpdateRequest
import java.net.URL

class SpreadsheetArtifact {
    companion object {
        val url = PROPERTIES.CREDENTIALS_URL.get()!!
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()!!
    }

    fun createArtifact(destinationTitle: String) : String {
        return SheetCopier(URL(url), spreadsheetId).copySpreadsheet(destinationTitle)
    }

    fun writeArtifact(spreadsheetId: String, requestData: Map<String, String>) {
        val updateRequests = requestData.map { (k, v) ->
            UpdateRequest(k, v)
        }
        return SheetWriter(URL(url), spreadsheetId).batchUpdateCells(updateRequests)
    }
}