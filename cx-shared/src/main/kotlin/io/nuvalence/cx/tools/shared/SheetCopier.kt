package io.nuvalence.cx.tools.shared

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import java.net.URL

/**
 * Copy operations for Google Sheets.
 *
 * @param credentialsURL URL pointing to where the credentials.json file is (e.g.
 * file:///path/to/credentials.json)
 * @param spreadsheetId the ID of the Google Sheet to copy (See SheetReader for details
 * about this parameter)
 */
class SheetCopier(credentialsURL: URL, private val spreadsheetId: String) {
    private val transport = GoogleNetHttpTransport.newTrustedTransport()
    private val service = Sheets.Builder(
        transport,
        GsonFactory.getDefaultInstance(),
        Authorizer(credentialsURL, transport).getCredentials()
    )
        .setApplicationName("Dialogflow Agent Generator")
        .build()

    /**
     * Copies a spreadsheet to a destination
     *
     * @param destinationTitle the title of the destination spreadsheet
     *
     * @return the spreadsheet ID of the newly generated spreadsheet
     */
    fun copySpreadsheet(destinationTitle: String): String {
        // Create a new spreadsheet
        val newSpreadsheet = Spreadsheet()
        val createdSpreadsheet = service.spreadsheets().create(newSpreadsheet).execute()
        val destinationSpreadsheetId = createdSpreadsheet.spreadsheetId

        // Rename new spreadsheet to specified title
        val renameRequest = BatchUpdateSpreadsheetRequest().setRequests(
            listOf(Request().setUpdateSpreadsheetProperties(
                UpdateSpreadsheetPropertiesRequest().setProperties(
                    SpreadsheetProperties().setTitle(destinationTitle)
                ).setFields("title")
            ))
        )
        service.spreadsheets().batchUpdate(destinationSpreadsheetId, renameRequest).execute()

        // Copy the all sheets from the source spreadsheet to the destination spreadsheet
        service.spreadsheets().get(spreadsheetId).execute()
            .sheets.forEachIndexed { index, sheet ->
                val request = CopySheetToAnotherSpreadsheetRequest()
                    .setDestinationSpreadsheetId(destinationSpreadsheetId)
                service.spreadsheets().sheets().copyTo(spreadsheetId, sheet.properties?.sheetId!!, request).execute()
                val newSheetId = service.spreadsheets().get(destinationSpreadsheetId).execute().sheets?.get(index+1)?.properties?.sheetId!!
                val updateRequest = BatchUpdateSpreadsheetRequest().setRequests(
                    listOf(Request().setUpdateSheetProperties(
                        UpdateSheetPropertiesRequest().setProperties(SheetProperties().setSheetId(newSheetId).setTitle(sheet.properties?.title)).setFields("title")
                    ))
                )
                service.spreadsheets().batchUpdate(destinationSpreadsheetId, updateRequest).execute()
            }

        return destinationSpreadsheetId
    }

}