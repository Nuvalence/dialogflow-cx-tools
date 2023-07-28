package io.nuvalence.cx.tools.cxtest.artifact

import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.shared.SheetCopier
import java.net.URL

class SpreadsheetArtifactCreator {
    companion object {
        val url = PROPERTIES.CREDENTIALS_URL.get()!!
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()!!
    }

    fun createArtifact(destinationTitle: String) : String {
        return SheetCopier(URL(url), spreadsheetId).copySpreadsheet(destinationTitle)
    }
}