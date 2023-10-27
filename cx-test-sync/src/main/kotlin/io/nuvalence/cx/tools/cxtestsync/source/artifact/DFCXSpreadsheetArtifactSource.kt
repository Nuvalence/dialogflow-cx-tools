package io.nuvalence.cx.tools.cxtestsync.source.artifact

import gov.ny.dol.ui.ccai.dfcx.domain.model.artifact.DFCXTestBuilderResultArtifactFormat as Model
import gov.ny.dol.ui.ccai.dfcx.domain.artifact.DFCXSpreadsheetArtifact
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.cxtestcore.Properties
import io.nuvalence.cx.tools.cxtestsync.model.test.*
import java.net.URL

class DFCXSpreadsheetArtifactSource {
    companion object {
        val url = Properties.getProperty<URL>("credentialsUrl")
        val spreadsheetId = Properties.getProperty<String>("spreadsheetId")
        val agentPath = Properties.getProperty<String>("agentPath")

        val rows : List<List<String>> = SheetReader(
            url, spreadsheetId, DFCXSpreadsheetArtifact.sheetTitle
        ).read()
        val cols : Map<String, Int>

        init {
            val headerRow = rows[0].map { item -> item.trim() }
            cols = Model.values().map{ it.headerName }.associateWith { colName -> headerRow.indexOf(colName) }
            cols.forEach { (colName, value) ->
                if(value == -1)
                    throw Error("Column $colName could not be found in the spreadsheet")
            }
        }

        fun getRowElement(colName: String, row: List<String>): String {
            val rowIndex = cols[colName]!!
            return if (rowIndex >= row.size) "" else row[rowIndex]
        }

        fun getRowElement(colName: Model, row: List<String>): String {
            return getRowElement(colName.headerName, row)
        }
    }

    fun getTestScenarios(): List<DFCXInjectableTest> {
        var currentTestCaseName = ""
        var currentTestCaseId = ""
        var currentTags = ""
        var currentNotes = ""
        var currentSsn = ""
        var testSteps = mutableListOf<DFCXInjectableTestStep>()

        val startingRow = 3

        val scenarios = rows.drop(2).foldIndexed(mutableListOf<DFCXInjectableTest>()) { index, acc, row ->
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
            } else if (getRowElement(Model.TEST_CASE_NAME, row).isNotEmpty()) {
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
                currentTestCaseName = getRowElement(Model.TEST_CASE_NAME, row)
                currentTestCaseId = "${agentPath}/testCases/${getRowElement(Model.TEST_CASE_ID, row)}"
                currentTags = getRowElement(Model.TAGS, row)
                currentNotes = getRowElement(Model.NOTES, row)
                currentSsn = getRowElement(Model.TEST_SSN, row)
            } else {
                // is test step
                testSteps.add(
                    DFCXInjectableTestStep(
                        getRowElement(Model.USER_INPUT, row),
                        getRowElement(Model.AGENT_OUTPUT, row),
                        mapOf("" to "") // TODO: implement payload parsing
                    )
                )
            }

            acc
        }

        return scenarios
    }
}
