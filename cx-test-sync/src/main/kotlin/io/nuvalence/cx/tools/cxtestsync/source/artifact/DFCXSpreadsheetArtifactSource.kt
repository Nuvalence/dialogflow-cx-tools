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

        // If test case found
            // If test steps are empty, it's probably the first step
                // Keep row in current
            // If test steps are NOT empty, it's probably a new test case
                // Persist current test case + steps
        // If test step found
            // Create new test step
        // If empty row found
            // Probably EOF
                // Persist current test case + steps

        val scenarios = rows.drop(1).foldIndexed(mutableListOf<DFCXTest>()) { index, acc, row ->
            fun getRowElement(colName: String): String {
                return row[cols[colName]!!]
            }

            fun processTags(tags: String): List<String> {
                return tags.split(",").map { it.trim() }
            }

            // is empty or EOF
            if (row.isEmpty() || index >= rows.size - startingRow) {
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
            } else {
                // is test case
                if (getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_NAME).isNotEmpty()) {
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
                    currentTestCaseName = getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_NAME)
                    currentTestCaseId = getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_ID)
                    currentTags = getRowElement(DFCXTestSpreadsheetModel.TAGS)
                    currentNotes = getRowElement(DFCXTestSpreadsheetModel.NOTES)
                }

                // is test step
                testSteps.add(
                    DFCXTestStep(
                        getRowElement(DFCXTestSpreadsheetModel.USER_INPUT),
                        getRowElement(DFCXTestSpreadsheetModel.AGENT_OUTPUT),
                        ResultLabel.valueOf(getRowElement(DFCXTestSpreadsheetModel.STATUS))
                    )
                )
            }

            acc
        }

        return scenarios
    }
}
