package io.nuvalence.cx.tools.shared

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import io.nuvalence.cx.tools.shared.format.CellFormatPreset
import io.nuvalence.cx.tools.shared.format.GridRangePreset
import io.nuvalence.cx.tools.shared.format.SheetPropertyPreset
import io.nuvalence.cx.tools.shared.format.TextHighlightPreset
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
     * Formats cells within an existing tab, presumed to be already populated. Columns are resized by the supplied parameter,
     * and cells are set to wrap text to make them more readable.
     *
     * @param sheetId the sheet ID of the tab
     * @param columnWidths list of sizes to resize the columns once the data is added
     */
    private fun setCellFormats(sheetId: Int, columnWidths: List<Int>) {
        columnWidths.forEachIndexed { i, columnWidth ->
            // Set column width
            val dimensionRange = DimensionRange()
                .setSheetId(sheetId)
                .setDimension("COLUMNS")
                .setStartIndex(i)
                .setEndIndex(i + 1)
            val dimensionProperties = DimensionProperties().setPixelSize(columnWidth)
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

    /**
     * Adds data to an existing tab. Columns are resized by the supplied parameter,
     * and cells are set to wrap text to make them more readable.
     *
     * @param tabName name of the tab
     * @param data rows of data to add to the tab
     * @param headers list of headers for the given data
     * @param columnWidths list of sizes to resize the columns once the data is added
     */
    fun addDataToTab(tabName: String, data: List<List<String>>, headers: List<String>, columnWidths: List<Int>) {
        val request = ValueRange().setValues(listOf(headers, *data.toTypedArray()))

        val sheet = service.spreadsheets().get(spreadsheetId).execute().sheets.find { it.properties.title == tabName }
        val sheetId = sheet?.properties?.sheetId ?: error("Could not find tab $tabName")

        service
            .spreadsheets()
            .values()
            .update(spreadsheetId, tabName, request)
            .setValueInputOption("RAW")
            .execute()

        setCellFormats(sheetId, columnWidths)
    }

    /**
     * Adds formatted data to an existing tab. Columns are resized by the supplied parameter,
     * and cells are set to wrap text to make them more readable.
     *
     * @param tabName name of the tab
     * @param data rows of data to add to the tab
     * @param headers list of headers for the given data
     * @param columnWidths list of sizes to resize the columns once the data is added
     * @param columnOffset column index to begin formatting on
     * @param highlightFormatRunIndices 2D table of format run indices per cell
     * @param highlightFormat text formatting to apply to highlighted text
     */
    fun addFormattedDataToTab(tabName: String, data: List<List<String>>, headers: List<String>, columnWidths: List<Int>,
                              columnOffset: Int, highlightFormatRunIndices: List<List<List<Pair<Int, Int>>>>, highlightFormat: TextHighlightPreset
    ) {
        val requests = mutableListOf<Request>()
        val dataWithHeader = listOf(headers, *data.toTypedArray())

        val sheet = service.spreadsheets().get(spreadsheetId).execute().sheets.find { it.properties.title == tabName }
        val sheetId = sheet?.properties?.sheetId ?: error("Could not find tab $tabName")

        dataWithHeader.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cellValue ->
                val textFormatRuns = mutableListOf<TextFormatRun>()

                if (rowIndex > 0 && columnIndex >= columnOffset) {
                    val highlightRow = highlightFormatRunIndices[rowIndex - 1]
                    if (columnIndex - columnOffset < highlightRow.size) {
                        val highlightRanges = highlightRow[columnIndex - columnOffset]
                        highlightRanges.forEach { range ->
                            textFormatRuns.add(
                                TextFormatRun().apply {
                                    startIndex = range.first
                                    format = highlightFormat.getHighlightFormat()
                                }
                            )
                            if (range.second + 1 < cellValue.length)
                                textFormatRuns.add(
                                    TextFormatRun().apply {
                                        startIndex = range.second + 1
                                        format = null
                                    }
                                )
                        }
                    }
                }

                val cellData = CellData().apply {
                    userEnteredValue = ExtendedValue().setStringValue(cellValue)
                    if (textFormatRuns.isNotEmpty()) {
                        this.textFormatRuns = textFormatRuns.sortedWith(compareBy<TextFormatRun> { it.startIndex }
                            .thenComparator {a, b ->
                                when {
                                    a.format == null && b.format != null -> -1
                                    a.format != null && b.format == null -> 1
                                    else -> 0
                                }
                            }
                        )
                    }
                }

                val gridRange = GridRange().apply {
                    this.sheetId = sheetId
                    this.startRowIndex = rowIndex
                    this.endRowIndex = rowIndex + 1
                    this.startColumnIndex = columnIndex
                    this.endColumnIndex = columnIndex + 1
                }

                requests.add(Request().setUpdateCells(UpdateCellsRequest().apply {
                    rows = listOf(RowData().setValues(listOf(cellData)))
                    range = gridRange
                    fields = "userEnteredValue,textFormatRuns"
                }))
            }
        }

        val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
        service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
        setCellFormats(sheetId, columnWidths)
    }

    fun deleteCellRange(range: String) {
        service.spreadsheets().values().clear(spreadsheetId, range, ClearValuesRequest()).execute()
    }

    fun applyCellFormatUpdates(tabName: String, cellFormatPreset: CellFormatPreset, gridRangePreset: GridRangePreset, gridRangeSpan: Int) {
        val sheet = service.spreadsheets().get(spreadsheetId).execute().sheets.find { it.properties.title == tabName }
        val sheetId = sheet?.properties?.sheetId ?: error("Could not find tab $tabName")

        val format = cellFormatPreset.getCellFormat()
        val range = gridRangePreset.getGridRange(sheetId, gridRangeSpan)

        val formatUpdateRequest = FormatUpdateRequest(range, format)
        batchUpdateCellFormats(listOf(formatUpdateRequest))
    }

    fun applyCellFormatUpdates(tabName: String, cellFormatPreset: CellFormatPreset, coordinates: List<String>) {
        val sheet = service.spreadsheets().get(spreadsheetId).execute().sheets.find { it.properties.title == tabName }
        val sheetId = sheet?.properties?.sheetId ?: error("Could not find tab $tabName")

        val format = cellFormatPreset.getCellFormat()
        val formatUpdateRequests = mutableListOf<FormatUpdateRequest>()
        coordinates.forEach { cellPosition ->
            val range = GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(cellPosition.substring(1).toInt() - 1)
                .setEndRowIndex(cellPosition.substring(1).toInt())
                .setStartColumnIndex(cellPosition[0] - 'A')
                .setEndColumnIndex(cellPosition[0] - 'A' + 1)
            formatUpdateRequests.add(FormatUpdateRequest(range, format))
        }
        batchUpdateCellFormats(formatUpdateRequests)
    }

    fun applySheetPropertyUpdates(tabName: String, sheetPropertyPreset: SheetPropertyPreset, gridPropertySpan: Int) {
        val sheet = service.spreadsheets().get(spreadsheetId).execute().sheets.find { it.properties.title == tabName }
        val sheetId = sheet?.properties?.sheetId ?: error("Could not find tab $tabName")

        val properties = sheetPropertyPreset.getSheetProperties(sheetId, gridPropertySpan)
        val fields = sheetPropertyPreset.getFields()

        val request = Request().setUpdateSheetProperties(UpdateSheetPropertiesRequest()
            .setProperties(properties)
            .setFields(fields)
        )

        batchUpdateSheets(listOf(request))
    }

    fun batchUpdateCellContents(cellContentUpdateRequests: List<CellContentUpdateRequest>) {
        val data = cellContentUpdateRequests.map { (sheetIdAndRange, value) ->
            ValueRange().setRange(sheetIdAndRange).setValues(listOf(listOf(value)))
        }

        val body = BatchUpdateValuesRequest().setValueInputOption("RAW").setData(data)
        val result = service.spreadsheets().values().batchUpdate(spreadsheetId, body).execute()

        println("${result.totalUpdatedCells ?: 0} cells updated.")
    }

    fun batchUpdateCellFormats(updateRequests: List<FormatUpdateRequest>) {
        val requests = ArrayList<Request>()

        updateRequests.forEach { request ->
            val repeatCellRequest = RepeatCellRequest()
                .setRange(request.range)
                .setCell(CellData().setUserEnteredFormat(request.format))
                .setFields("userEnteredFormat(backgroundColor,textFormat,wrapStrategy,verticalAlignment)")

            requests.add(Request().setRepeatCell(repeatCellRequest))
        }

        val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
        service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
    }

    fun batchUpdateSheets(requests: List<Request>) {
        val updateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
        service.spreadsheets().batchUpdate(spreadsheetId, updateRequest).execute()
    }

    fun addEmptyRows(numberOfRows: Int, spreadsheetId: String, sheetName: String, sheetId: Int, startRowIndex: Int = 0) {
        val response = service.spreadsheets().values().get(spreadsheetId, "$sheetName!A:A").execute()
        val lastRow = response.getValues()?.size ?: startRowIndex

        val request = Request()
            .setInsertDimension(InsertDimensionRequest()
                .setRange(DimensionRange()
                    .setSheetId(sheetId)
                    .setDimension("ROWS")
                    .setStartIndex(lastRow)
                    .setEndIndex(lastRow + numberOfRows))
            )

        val batchUpdate = service.spreadsheets().batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(listOf(request)))
        batchUpdate.execute()
    }
}

data class CellContentUpdateRequest (val cellAddress: String, val data: String)

data class FormatUpdateRequest (val range: GridRange, val format: CellFormat)