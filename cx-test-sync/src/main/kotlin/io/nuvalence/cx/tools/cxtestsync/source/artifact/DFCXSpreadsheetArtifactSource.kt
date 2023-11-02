package io.nuvalence.cx.tools.cxtestsync.source.artifact

import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.cxtestsync.model.artifact.DFCXTestSpreadsheetModel
import io.nuvalence.cx.tools.cxtestsync.model.test.*
import io.nuvalence.cx.tools.cxtestsync.util.Properties

class DFCXSpreadsheetArtifactSource {
    companion object {
        val url = Properties.CREDENTIALS_URL
        val spreadsheetId = Properties.SPREADSHEET_ID
        val agentPath = Properties.AGENT_PATH

        lateinit var cols : Map<String, Int>
    }

    fun getTestScenarios(): List<DFCXInjectableTest> {
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
        var currentSsn = ""
        var testSteps = mutableListOf<DFCXInjectableTestStep>()

        val startingRow = 3

        val scenarios = rows.drop(2).foldIndexed(mutableListOf<DFCXInjectableTest>()) { index, acc, row ->
            fun getRowElement(colName: String): String {
                val rowIndex = cols[colName]!!
                return if (rowIndex >= row.size) "" else row[rowIndex]
            }

            fun processTags(tags: String): List<String> {
                return tags.split("\n").map { it.trim() }
            }

            if (row.isEmpty() || index >= rows.size - startingRow) {
                // is empty or EOF
                acc.add(
                    DFCXInjectableTest(
                        currentTestCaseId,
                        currentTestCaseName,
                        processTags(currentTags),
                        currentNotes,
                        testSteps,
                        currentSsn
                    )
                )
                testSteps = mutableListOf()
            } else if (getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_NAME).isNotEmpty()) {
                // is test case
                if (testSteps.isNotEmpty()) {
                    acc.add(
                        DFCXInjectableTest(
                            currentTestCaseId,
                            currentTestCaseName,
                            processTags(currentTags),
                            currentNotes,
                            testSteps,
                            currentSsn
                        )
                    )
                    testSteps = mutableListOf()
                }
                currentTestCaseName = getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_NAME)
                currentTestCaseId = "${agentPath}/testCases/${getRowElement(DFCXTestSpreadsheetModel.TEST_CASE_ID)}"
                currentTags = getRowElement(DFCXTestSpreadsheetModel.TAGS)
                currentNotes = getRowElement(DFCXTestSpreadsheetModel.NOTES)
                currentSsn = getRowElement(DFCXTestSpreadsheetModel.TEST_SSN)
            } else {
                // is test step
                testSteps.add(
                    DFCXInjectableTestStep(
                        getRowElement(DFCXTestSpreadsheetModel.USER_INPUT),
                        getRowElement(DFCXTestSpreadsheetModel.AGENT_OUTPUT),
                        mapOf("" to "") // TODO: implement payload parsing
                    )
                )
            }

            acc
        }

        return scenarios
    }
}
