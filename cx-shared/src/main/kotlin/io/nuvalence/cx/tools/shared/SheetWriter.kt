package io.nuvalence.cx.tools.shared

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import java.net.URL

/**
 * Write operations to Google Sheets.
 *
 * @param credentialsURL URL pointing to where the credentials.json file is (e.g.
 * file:///path/to/credentials.json)
 * @param spreadsheetId the ID of the Google Sheet (See SheetReader for details
 * about this parameter)
 */
class SheetWriter(credentialsURL: URL, private val spreadsheetId: String) {
    private val transport = GoogleNetHttpTransport.newTrustedTransport()
    private val service = Sheets.Builder(
        transport,
        GsonFactory.getDefaultInstance(),
        Authorizer(credentialsURL, transport).getCredentials()
    )
        .setApplicationName("Dialogflow Agent Generator")
        .build()

    /**
     * Deletes a tab.
     *
     * @param tabName the name of the tab to delete
     */
    fun deleteTab(tabName: String) {
        service.spreadsheets().get(spreadsheetId).execute().sheets.find { sheet ->
            sheet.properties.title == tabName
        }?.let { sheet ->
            val deleteRequest = DeleteSheetRequest().setSheetId(sheet.properties.sheetId)
            service.spreadsheets().batchUpdate(spreadsheetId,
                BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(Request().setDeleteSheet(deleteRequest)))
            ).execute()
        }
    }

    /**
     * Adds a tab.
     *
     * @param tabName the name of the tab to add
     */
    fun addTab(tabName: String) {
        val request = listOf(
            Request().setAddSheet(
                AddSheetRequest().setProperties(SheetProperties().setTitle(tabName))
            )
        )
        val body: BatchUpdateSpreadsheetRequest = BatchUpdateSpreadsheetRequest().setRequests(request)
        service.spreadsheets().batchUpdate(spreadsheetId, body).execute()
    }

    /**
     * Adds data to an existing tab. Columns are resized by the supplied parameter,
     * and cells are set to wrap text to make them more readable.
     *
     * @param tabName name of the tab
     * @param data rows of data to add to the tab
     * @param headers list of headers for the given data
     * @param sizes list of sizes to resize the columns once the data is added.
     */
    fun addDataToTab(tabName: String, data: List<List<String>>, headers: List<String>, sizes: List<Int>) {
        val request = ValueRange().setValues(listOf(headers, *data.toTypedArray()))
        service
            .spreadsheets()
            .values()
            .update(spreadsheetId, tabName, request)
            .setValueInputOption("RAW")
            .execute()
        val sheet = service.spreadsheets().get(spreadsheetId).execute().sheets.find { it.properties.title == tabName }
        val sheetId = sheet?.properties?.sheetId ?: error("Could not find tab $tabName")
        sizes.forEachIndexed { i, size ->
            // Set column width
            val dimensionRange = DimensionRange()
                .setSheetId(sheetId)
                .setDimension("COLUMNS")
                .setStartIndex(i)
                .setEndIndex(i + 1)
            val dimensionProperties = DimensionProperties().setPixelSize(size)
            val updateDimensionPropertiesRequest = UpdateDimensionPropertiesRequest()
                .setRange(dimensionRange)
                .setProperties(dimensionProperties)
                .setFields("pixelSize")
            // Set cell wrap to make long text more readable
            val wrapText = CellFormat().setWrapStrategy("WRAP")
            val cellData = CellData().setUserEnteredFormat(wrapText)
            val wrapTextRequest = RepeatCellRequest()
                .setRange(GridRange().setSheetId(sheetId))
                .setCell(cellData)
                .setFields("userEnteredFormat.wrapStrategy")
            // Do it!
            service.spreadsheets().batchUpdate(
                spreadsheetId,
                BatchUpdateSpreadsheetRequest()
                    .setRequests(
                        listOf(
                            Request().setUpdateDimensionProperties(updateDimensionPropertiesRequest),
                            Request().setRepeatCell(wrapTextRequest)
                        )
                    )
            ).execute()
        }
    }

    fun batchUpdateCells(updateRequests: List<UpdateRequest>) {
        val data = updateRequests.map { (sheetIdAndRange, value) ->
            ValueRange().setRange(sheetIdAndRange).setValues(listOf(listOf(value)))
        }

        // Create a BatchUpdateValuesRequest object and set the data
        val body = BatchUpdateValuesRequest().setValueInputOption("RAW").setData(data)

        // Execute the batch update request
        val result = service.spreadsheets().values().batchUpdate(spreadsheetId, body).execute()

        println("${result.totalUpdatedCells ?: 0} cells updated.")
    }

    fun batchUpdateSheets(requests: List<Request>) {
        // Create a BatchUpdateSpreadsheetRequest
        val updateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
        service.spreadsheets().batchUpdate(spreadsheetId, updateRequest).execute()
    }
}

data class UpdateRequest (val cellAddress: String, val data: String)