package io.nuvalence.cx.tools.shared

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import java.net.URL

/**
 * Create operations to Google Sheets.
 *
 * @param credentialsURL URL pointing to where the credentials.json file is (e.g.
 * file:///path/to/credentials.json)
 */
class SheetCreator(credentialsURL: URL) {
    private val transport = GoogleNetHttpTransport.newTrustedTransport()
    private val service = Sheets.Builder(
        transport,
        GsonFactory.getDefaultInstance(),
        Authorizer(credentialsURL, transport).getCredentials()
    )
        .setApplicationName("Dialogflow Agent Generator")
        .build()

    fun createNewSpreadsheet(title: String): String {
        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle(title))

        val createdSpreadsheet = service.spreadsheets().create(spreadsheet).execute()

        return createdSpreadsheet.spreadsheetId
    }
}
