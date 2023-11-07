package io.nuvalence.cx.tools.cxtest.artifact

import com.google.api.services.sheets.v4.model.*
import com.google.cloud.dialogflow.cx.v3.TestRunDifference
import io.nuvalence.cx.tools.cxtest.model.artifact.ResultArtifactFormat
import io.nuvalence.cx.tools.cxtest.model.artifact.ResultDetails
import io.nuvalence.cx.tools.cxtest.model.artifact.ResultLabelFormat
import io.nuvalence.cx.tools.cxtest.model.test.DFCXTestBuilderResult
import io.nuvalence.cx.tools.cxtest.model.test.ResultLabel
import io.nuvalence.cx.tools.cxtest.util.Properties
import io.nuvalence.cx.tools.shared.*
import kotlin.properties.Delegates

class DFCXSpreadsheetArtifact {
    companion object {
        val url = Properties.CREDENTIALS_URL
        var resultSheetId by Delegates.notNull<Int>()
        var summarySheetId by Delegates.notNull<Int>()

        const val resultSheetTitle = "Test Results"
        const val summarySheetTitle = "Summary"

        private const val DATA_START_ROW = 2
    }

    fun createArtifact(title: String) : String {
        val spreadsheetId = SheetCreator(url).createNewSpreadsheet(title)
        SheetWriter(url, spreadsheetId).addTab(summarySheetTitle)
        val sheets = SheetReader(url, spreadsheetId, "").getSheets()
        resultSheetId = sheets.firstOrNull()?.properties?.sheetId!!
        summarySheetId = sheets.lastOrNull()?.properties?.sheetId!!
        initializeSheets(spreadsheetId)
        return spreadsheetId
    }

    fun writeArtifact(spreadsheetId: String, formattedResultsList: List<DFCXTestBuilderResult>) {
        val sheetWriter = SheetWriter(url, spreadsheetId)

        // Gather total rows
        val totalRowCount = formattedResultsList.fold(0) { acc, result -> acc + result.resultSteps.size }
        sheetWriter.addEmptyRows(totalRowCount+1, spreadsheetId, resultSheetTitle, resultSheetId, DATA_START_ROW)

        // Header row
        val requestData = ResultArtifactFormat.values().map { it.headerName }.withIndex().associate { e ->
            "${resultSheetTitle}!${'A' + e.index}1" to e.value
        }.toMutableMap()

        val resultDetails = mutableListOf<ResultDetails>()
        var rowCounter = DATA_START_ROW + 1;
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

        sheetWriter.batchUpdateSheets(requests)
    }

    private fun initializeSheets(destinationSpreadsheetId: String) {
        val sheetWriter = SheetWriter(url, destinationSpreadsheetId)

        initializeResultSheet(sheetWriter)
        initializeSummarySheet(sheetWriter)
    }

    private fun initializeSummarySheet(sheetWriter: SheetWriter) {

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
            .setStartRowIndex(1).setEndRowIndex(DATA_START_ROW)
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
                .setStartRowIndex(DATA_START_ROW).setEndRowIndex(null)
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
            .setStartRowIndex(DATA_START_ROW).setEndRowIndex(null)
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