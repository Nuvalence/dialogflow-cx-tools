package io.nuvalence.cx.tools.cxtest.artifact

import com.google.api.services.sheets.v4.model.*
import com.google.cloud.dialogflow.cx.v3.*
import io.nuvalence.cx.tools.cxtest.model.artifact.ResultArtifactFormat
import io.nuvalence.cx.tools.cxtest.model.artifact.ResultDetails
import io.nuvalence.cx.tools.cxtest.model.artifact.ResultLabelFormat
import io.nuvalence.cx.tools.cxtest.model.artifact.SummaryArtifactFormat
import io.nuvalence.cx.tools.cxtest.model.test.DFCXTestBuilderResult
import io.nuvalence.cx.tools.cxtest.model.test.ResultLabel
import io.nuvalence.cx.tools.cxtest.util.Properties
import io.nuvalence.cx.tools.shared.*
import kotlin.properties.Delegates

class DFCXSpreadsheetArtifact {
    companion object {
        val url = Properties.CREDENTIALS_URL
        val summaryInfo = SummaryInfo(Properties.AGENT_PATH)
        var resultSheetId by Delegates.notNull<Int>()
        var summarySheetId by Delegates.notNull<Int>()

        const val resultSheetTitle = "Test Results"
        const val summarySheetTitle = "Summary"

        private const val RESULT_DATA_START_ROW = 2
        private const val SUMMARY_DATA_START_ROW = 1
    }

    data class SummaryInfo(val agentPath: String) {
        lateinit var agentName: String
        lateinit var testTimestamp: String
        lateinit var tagsIncluded: String
        lateinit var tagsExcluded: String
        var testsRun by Delegates.notNull<Int>()
        var testsPassed by Delegates.notNull<Int>()
        var testsFailed by Delegates.notNull<Int>()
        lateinit var transitionsCoverage: String
        lateinit var intentsCoverage: String
        lateinit var routeGroupsCoverage: String

        fun getMap() : Map<String, String> {
            return mapOf(
                "Agent Path" to agentPath,
                "Agent Name" to agentName,
                "Test Timestamp" to testTimestamp,
                "Tags Included" to tagsIncluded,
                "Tags Excluded" to tagsExcluded,
                "Tests Run" to testsRun.toString(),
                "Tests Passed" to testsPassed.toString(),
                "Tests Failed" to testsFailed.toString(),
                "Transitions" to transitionsCoverage,
                "Intents" to intentsCoverage,
                "Route Groups" to routeGroupsCoverage
            )
        }
    }

    /**
     * Creates a new spreadsheet artifact and returns the spreadsheet ID
     *
     * @param title the title of the spreadsheet
     * @return the spreadsheet ID
     */
    fun createArtifact(title: String) : String {
        val spreadsheetId = SheetCreator(url).createNewSpreadsheet(title)
        SheetWriter(url, spreadsheetId).addTab(summarySheetTitle)
        val sheets = SheetReader(url, spreadsheetId, "").getSheets()
        resultSheetId = sheets[0].properties?.sheetId!!
        summarySheetId = sheets[1].properties?.sheetId!!
        initializeSheets(spreadsheetId)
        return spreadsheetId
    }

    /**
     * Writes the results to the spreadsheet
     *
     * @param spreadsheetId the ID of the spreadsheet to write to
     * @param formattedResultsList the list of formatted results to write
     */
    fun writeArtifact(spreadsheetId: String, formattedResultsList: List<DFCXTestBuilderResult>) {
        val sheetWriter = SheetWriter(url, spreadsheetId)

        try {
            writeArtifactResults(sheetWriter, spreadsheetId, formattedResultsList)
            writeArtifactSummary(sheetWriter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeArtifactResults(sheetWriter: SheetWriter, spreadsheetId: String, formattedResultsList: List<DFCXTestBuilderResult>) {
        // Gather total rows
        val totalRowCount = formattedResultsList.fold(0) { acc, result -> acc + result.resultSteps.size }
        sheetWriter.addEmptyRows(totalRowCount+1, spreadsheetId, resultSheetTitle, resultSheetId, RESULT_DATA_START_ROW)

        // Header row
        val requestData = ResultArtifactFormat.values().map { it.headerName }.withIndex().associate { e ->
            "${resultSheetTitle}!${'A' + e.index}1" to e.value
        }.toMutableMap()

        val resultDetails = mutableListOf<ResultDetails>()
        var rowCounter = RESULT_DATA_START_ROW + 1
        // For each test
        formattedResultsList.sortedBy { result -> result.testCaseId } .forEach { result ->
            // Add display name, tags, notes
            requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.TEST_CASE_NAME.ordinal}${rowCounter}", result.testCaseName)
            requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.TEST_CASE_ID.ordinal}${rowCounter}", result.testCaseId.split("/")[7])
            requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.TAGS.ordinal}${rowCounter}", result.tags.joinToString("\n"))
            requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.NOTES.ordinal}${rowCounter}", result.notes)
            requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.TEST_RESULT.ordinal}${rowCounter}", result.result.value)

            rowCounter++

            // For each step
            // Add user input, agent output, status, error details
            result.resultSteps.forEach { resultStep ->
                requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.USER_INPUT.ordinal}${rowCounter}", resultStep.userInput)
                requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.AGENT_OUTPUT.ordinal}${rowCounter}", resultStep.expectedAgentOutput)
                requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.TEST_RESULT.ordinal}${rowCounter}", resultStep.result.value)

                if (resultStep.result == ResultLabel.FAIL) {
                    val message = resultStep.diffs.joinToString("\n-----\n") {
                        when (it.type) {
                            TestRunDifference.DiffType.UTTERANCE -> "${it.description}: ${resultStep.actualAgentOutput}"
                            else -> it.description
                        }
                    }
                    requestData += Pair("${resultSheetTitle}!${'A' + ResultArtifactFormat.TEST_RESULT_DETAILS.ordinal}${rowCounter}", message)
                    resultDetails.add(ResultDetails(message, rowCounter-1, ResultArtifactFormat.TEST_RESULT_DETAILS.ordinal))
                }

                rowCounter++
            }
        }

        val cellContentUpdateRequests = requestData.map { (k, v) ->
            CellContentUpdateRequest(k, v)
        }

        sheetWriter.batchUpdateCellContents(cellContentUpdateRequests)

        boldResultDetails(sheetWriter, resultDetails)
    }

    private fun writeArtifactSummary(sheetWriter: SheetWriter) {
        // Header row
        val requestData = SummaryArtifactFormat.values().map { it.categoryName }.withIndex().associate { e ->
            "${summarySheetTitle}!${'A' + e.index * 2}1" to e.value
        }.toMutableMap()

        // Data column labels
        SummaryArtifactFormat.values().forEach { category ->
            category.dataPoints.forEachIndexed { index, dataPoint ->
                // Data label
                requestData += Pair("${summarySheetTitle}!${'A' + category.ordinal * 2}${SUMMARY_DATA_START_ROW + index + 1}", dataPoint.labelName)

                // Data
                val data = summaryInfo.getMap().getValue(dataPoint.labelName)
                requestData += Pair("${summarySheetTitle}!${'A' + category.ordinal * 2 + 1}${SUMMARY_DATA_START_ROW + index + 1}", data)
            }
        }

        val cellContentUpdateRequests = requestData.map { (k, v) ->
            CellContentUpdateRequest(k, v)
        }

        sheetWriter.batchUpdateCellContents(cellContentUpdateRequests)
    }

    private fun boldResultDetails(sheetWriter: SheetWriter, resultDetails: List<ResultDetails>) {
        val pageMismatchSubstring = "Page mismatch:"
        val agentResponseMismatchSubstring = "Agent response mismatch:"

        val substrings = listOf(pageMismatchSubstring, agentResponseMismatchSubstring)

        val requests = mutableListOf<Request>()
        resultDetails.forEach { resultDetail ->
            val formatRuns = mutableListOf<TextFormatRun>()
            for (substring in substrings) {
                var index = 0
                while (index < resultDetail.message.length) {
                    val foundIndex = resultDetail.message.indexOf(substring, index)
                    if (foundIndex == -1) break
                    formatRuns.add(
                        TextFormatRun()
                            .setStartIndex(foundIndex)
                            .setFormat(TextFormat().setBold(true))
                    )
                    index = foundIndex + substring.length
                    formatRuns.add(
                        TextFormatRun()
                            .setStartIndex(index)
                            .setFormat(TextFormat().setBold(false))
                    )
                }
            }
            // Set the formatted value
            val cellData = CellData()
                .setUserEnteredValue(ExtendedValue().setStringValue(resultDetail.message))
                .setTextFormatRuns(formatRuns)

            val gridCoordinate = GridCoordinate()
                .setSheetId(resultSheetId)
                .setRowIndex(resultDetail.row)
                .setColumnIndex(resultDetail.column)

            requests.add(Request()
                .setUpdateCells(
                    UpdateCellsRequest()
                        .setStart(gridCoordinate)
                        .setRows(listOf(RowData().setValues(listOf(cellData))))
                        .setFields("userEnteredValue,textFormatRuns")
                )
            )
        }

        if (requests.isNotEmpty()) {
            sheetWriter.batchUpdateSheets(requests)
        }
    }

    private fun initializeSheets(destinationSpreadsheetId: String) {
        val sheetWriter = SheetWriter(url, destinationSpreadsheetId)

        initializeResultSheet(sheetWriter)
        initializeSummarySheet(sheetWriter)
    }

    private fun initializeSummarySheet(sheetWriter: SheetWriter) {
        // Format header
        val headerFormat = CellFormat()
            .setBackgroundColor(Color().setRed(0.8f).setGreen(0.8f).setBlue(0.8f))
            .setTextFormat(TextFormat().setBold(true))
        val headerRange = GridRange().setSheetId(summarySheetId)
            .setStartRowIndex(0).setEndRowIndex(1)
        val headerRequest = FormatUpdateRequest(headerRange, headerFormat)

        // Format label columns
        val labelColumnFormat = CellFormat().setTextFormat(TextFormat().setBold(true))
        val labelColumnRequests = SummaryArtifactFormat.values().mapIndexed { index, _ ->
            val labelColumnRange = GridRange().setSheetId(summarySheetId)
                .setStartColumnIndex(index*2).setEndColumnIndex(index*2 + 1)
            FormatUpdateRequest(labelColumnRange, labelColumnFormat)
        }

        // Format data columns
        val dataColumnFormat = CellFormat().setNumberFormat(NumberFormat().setType("TEXT"))
        val dataColumnRequests = SummaryArtifactFormat.values().mapIndexed { index, _ ->
            val dataColumnRange = GridRange().setSheetId(summarySheetId)
                .setStartColumnIndex(index*2 + 1).setEndColumnIndex(index*2 + 2)
            FormatUpdateRequest(dataColumnRange, dataColumnFormat)
        }

        // Add colors if present
        val colorRequests = SummaryArtifactFormat.values().fold(mutableListOf<FormatUpdateRequest>()) { requests, category ->
            category.dataPoints.forEachIndexed { index, dataPoint ->
                if (dataPoint.highlightColor != null) {
                    val dataPointRange = GridRange().setSheetId(summarySheetId)
                        .setStartRowIndex(SUMMARY_DATA_START_ROW + index).setEndRowIndex(SUMMARY_DATA_START_ROW + index + 1)
                        .setStartColumnIndex(category.ordinal * 2).setEndColumnIndex(category.ordinal * 2 + 2)
                    val dataPointFormat = CellFormat().setBackgroundColor(dataPoint.highlightColor)
                    val dataPointRequest = FormatUpdateRequest(dataPointRange, dataPointFormat)
                    requests.add(dataPointRequest)
                }
            }

            requests
        }

        // Set column widths
        val dataCellDimensionRequests = SummaryArtifactFormat.values().map { format ->
            Request().setUpdateDimensionProperties(
                UpdateDimensionPropertiesRequest()
                    .setRange(DimensionRange()
                        .setSheetId(summarySheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(format.ordinal * 2)
                        .setEndIndex(format.ordinal * 2 + 1)
                    )
                    .setProperties(DimensionProperties().setPixelSize(format.labelWidth))
                    .setFields("pixelSize")
            )
        } + SummaryArtifactFormat.values().map { format ->
            Request().setUpdateDimensionProperties(
                UpdateDimensionPropertiesRequest()
                    .setRange(DimensionRange()
                        .setSheetId(summarySheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(format.ordinal * 2 + 1)
                        .setEndIndex(format.ordinal * 2 + 2)
                    )
                    .setProperties(DimensionProperties().setPixelSize(format.dataWidth))
                    .setFields("pixelSize")
            )
        }

        // Freeze header row
        val freezeHeaderRequest = Request().setUpdateSheetProperties(UpdateSheetPropertiesRequest()
            .setProperties(SheetProperties().setSheetId(summarySheetId).setGridProperties(GridProperties().setFrozenRowCount(1)))
            .setFields("gridProperties.frozenRowCount")
        )

        sheetWriter.batchUpdateCellFormats(listOf(*labelColumnRequests.toTypedArray(), *dataColumnRequests.toTypedArray(), *colorRequests.toTypedArray()))
        sheetWriter.batchUpdateSheets(listOf(freezeHeaderRequest, *dataCellDimensionRequests.toTypedArray()))
        sheetWriter.batchUpdateCellFormats(listOf(headerRequest)) // Must be done after freeze header
    }

    private fun initializeResultSheet(sheetWriter: SheetWriter) {
        // Update result sheet name
        sheetWriter.batchUpdateSheets(listOf(
            Request().setUpdateSheetProperties(
                UpdateSheetPropertiesRequest().setProperties(SheetProperties().setSheetId(resultSheetId).setTitle(
                    resultSheetTitle)).setFields("title")
            )))

        // Format result sheet
        val headerFormat = CellFormat()
            .setBackgroundColor(Color().setRed(0.8f).setGreen(0.8f).setBlue(0.8f))
            .setTextFormat(TextFormat().setBold(true))
        val headerRange = GridRange().setSheetId(resultSheetId)
            .setStartRowIndex(0).setEndRowIndex(1)
            .setStartColumnIndex(0).setEndColumnIndex(ResultArtifactFormat.values().size)
        val headerRequest = FormatUpdateRequest(headerRange, headerFormat)

        val separatorFormat = CellFormat().setBackgroundColor(Color().setRed(0.8f).setGreen(1.0f).setBlue(0.8f))
        val separatorRange = GridRange().setSheetId(resultSheetId)
            .setStartRowIndex(1).setEndRowIndex(RESULT_DATA_START_ROW)
            .setStartColumnIndex(0).setEndColumnIndex(ResultArtifactFormat.values().size)
        val separatorRequest = FormatUpdateRequest(separatorRange, separatorFormat)

        val dataFormatRequests = mutableListOf<FormatUpdateRequest>()
        val dataCellDimensionRequests = mutableListOf<Request>()

        ResultArtifactFormat.values().forEach { format ->
            val dataFormat = CellFormat()
                .setWrapStrategy(format.wrapStrategy)
                .setVerticalAlignment("TOP")
            if (format.isMetadata) {
                dataFormat.textFormat = TextFormat().setBold(true)
            }

            val dataRange = GridRange().setSheetId(resultSheetId)
                .setStartRowIndex(RESULT_DATA_START_ROW).setEndRowIndex(null)
                .setStartColumnIndex(format.ordinal).setEndColumnIndex(format.ordinal + 1)

            val dataCellDimensionRequest = Request().setUpdateDimensionProperties(
                UpdateDimensionPropertiesRequest()
                    .setRange(DimensionRange()
                        .setSheetId(resultSheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(format.ordinal)
                        .setEndIndex(format.ordinal + 1)
                    )
                    .setProperties(DimensionProperties().setPixelSize(format.width))
                    .setFields("pixelSize")
            )

            dataFormatRequests.add(FormatUpdateRequest(dataRange, dataFormat))
            dataCellDimensionRequests.add(dataCellDimensionRequest)
        }

        // Format dropdowns
        val dropdownRequests = mutableListOf<Request>()

        val resultRange = GridRange().setSheetId(resultSheetId)
            .setStartRowIndex(RESULT_DATA_START_ROW).setEndRowIndex(null)
            .setStartColumnIndex(ResultArtifactFormat.TEST_RESULT.ordinal).setEndColumnIndex(ResultArtifactFormat.TEST_RESULT.ordinal + 1)

        ResultLabelFormat.values().forEach { entry ->
            val booleanCondition = BooleanCondition()
                .setType("TEXT_EQ")
                .setValues(listOf(ConditionValue().setUserEnteredValue(entry.data.value)))

            val booleanRule = BooleanRule()
                .setCondition(booleanCondition)
                .setFormat(CellFormat().setBackgroundColor(entry.color))

            val dropdownFormatRequest = Request().setAddConditionalFormatRule(AddConditionalFormatRuleRequest()
                .setRule(ConditionalFormatRule().setBooleanRule(booleanRule).setRanges(listOf(resultRange)))
                .setIndex(0))

            dropdownRequests.add(dropdownFormatRequest)
        }

        val resultDataValidationRule = DataValidationRule()
            .setCondition(BooleanCondition().setType("ONE_OF_LIST").setValues(ResultLabel.values().map { ConditionValue().setUserEnteredValue(it.value) }))
            .setShowCustomUi(true)
            .setStrict(true)

        val dropdownResultRequest = Request()
            .setSetDataValidation(SetDataValidationRequest()
                .setRange(resultRange)
                .setRule(resultDataValidationRule))

        dropdownRequests.add(dropdownResultRequest)

        sheetWriter.batchUpdateCellFormats(listOf(headerRequest, separatorRequest, *dataFormatRequests.toTypedArray()))
        sheetWriter.batchUpdateSheets(dataCellDimensionRequests)
        sheetWriter.batchUpdateSheets(dropdownRequests)
    }

}