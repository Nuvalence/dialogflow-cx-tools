package io.nuvalence.cx.tools.cxtestsync.source.artifact

import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTest
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTestSpreadsheetModel
import io.nuvalence.cx.tools.cxtestsync.model.DFCXTestStep
import io.nuvalence.cx.tools.cxtestsync.model.ResultLabel
import io.nuvalence.cx.tools.cxtestsync.util.Properties

class DFCXSpreadsheetArtifactSource {
    companion object {
        val url = Properties.CREDENTIALS_URL
        val spreadsheetId = Properties.SPREADSHEET_ID

        lateinit var cols : Map<String, Int>
    }

    fun getTestScenarios(): List<DFCXTest> {
        val rows = SheetReader(
            url, spreadsheetId, DFCXTestSpreadsheetModel.sheetTitle
        ).read()

        val headerRow = rows[0].map { item -> item.trim() }
        cols = DFCXTestSpreadsheetModel.colNames.associateWith { colName -> headerRow.indexOf(colName) }
        cols.forEach { (colName, value) ->
            if(value == -1)
                throw Error("Column $colName could not be found in the spreadsheet")
        }

        var currentTestCaseName = ""
        var currentTestCaseId = ""
        var currentTags = ""
        var currentNotes = ""
        var testSteps = mutableListOf<DFCXTestStep>()

        val startingRow = 2

        val scenarios = rows.drop(1).foldIndexed(mutableListOf<DFCXTest>()) { index, acc, row ->
            fun getRowElement(colName: String): String {
                return row[cols[colName]!!]
            }

            fun processTags(tags: String): List<String> {
                return tags.split(",").map { it.trim() }
            }

            if (row.isEmpty() || index == rows.size - startingRow) {
                if (testSteps.isNotEmpty()) {
                    acc.add(
                        DFCXTest(
                            currentTestCaseId,
                            currentTestCaseName,
                            processTags(currentTags),
                            currentNotes,
                            testSteps
                        )
                    )
                    testSteps = mutableListOf()
                }
            } else {
                if (getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_NAME).isNotEmpty()) {
                    currentTestCaseName = getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_NAME)
                    currentTestCaseId = getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_ID)
                    currentTags = getRowElement(DFCXTestSpreadsheetModel.TAGS)
                    currentNotes = getRowElement(DFCXTestSpreadsheetModel.NOTES)
                    testSteps.add(
                        DFCXTestStep(
                            getRowElement(DFCXTestSpreadsheetModel.USER_INPUT),
                            getRowElement(DFCXTestSpreadsheetModel.AGENT_OUTPUT),
                            ResultLabel.valueOf(DFCXTestSpreadsheetModel.STATUS)
                        )
                    )
                } else {
                    testSteps.add(
                        DFCXTestStep(
                            getRowElement(DFCXTestSpreadsheetModel.USER_INPUT),
                            getRowElement(DFCXTestSpreadsheetModel.AGENT_OUTPUT),
                            ResultLabel.valueOf(DFCXTestSpreadsheetModel.STATUS)
                        )
                    )
                    if (index == rows.size - startingRow + 1) {
                        acc.add(
                            DFCXTest(
                                currentTestCaseId,
                                currentTestCaseName,
                                processTags(currentTags),
                                currentNotes,
                                testSteps
                            )
                        )
                        testSteps = mutableListOf()
                    }
                }
            }

            acc
        }

        return scenarios
    }
}
