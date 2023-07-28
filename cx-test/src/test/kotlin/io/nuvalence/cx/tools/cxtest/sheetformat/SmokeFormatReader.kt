package io.nuvalence.cx.tools.cxtest.sheetformat

import io.nuvalence.cx.tools.cxtest.model.TestScenario
import io.nuvalence.cx.tools.cxtest.model.TestStep
import io.nuvalence.cx.tools.cxtest.util.ACTION_MAPPINGS
import io.nuvalence.cx.tools.cxtest.util.LANGUAGE_MAPPINGS
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.shared.SheetReader
import java.net.URL

class SmokeFormatReader {
    companion object {
        const val TEST_CASE_ID = "Test Case ID"
        const val TEST_CASE_LANGUAGE = "Language"
        const val TEST_CASE_TITLE = "Test Description"
        const val USER_INPUT = "Test Steps / User Input"
        const val EXPECTED_RESULT = "Expected Telephony Result"

        val colNames = listOf(TEST_CASE_ID, TEST_CASE_LANGUAGE, TEST_CASE_TITLE, USER_INPUT, EXPECTED_RESULT)
    }

    private fun createTestScenario(
        testSteps: List<TestStep>, range: String, title: String, testCaseId: String, testCaseLanguage: String
    ): TestScenario {
        return TestScenario("$range - $title - $testCaseId", testSteps.toList(), LANGUAGE_MAPPINGS.getValue(testCaseLanguage))
    }

    fun listSheets(prefix: String): List<String> {
        val url = PROPERTIES.CREDENTIALS_URL.get()
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()
        return SheetReader(URL(url), spreadsheetId, "").listSheets().filter { sheetName -> sheetName.startsWith(prefix) }
    }

    fun read(range: String): List<TestScenario> {
        val url = PROPERTIES.CREDENTIALS_URL.get()
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()
        val rows = SheetReader(
            URL(url), spreadsheetId, range
        ).read()

        val headerRow = rows[0].map { item -> item.trim() }
        val cols = colNames.associateWith { colName -> headerRow.indexOf(colName) }
        cols.forEach { (colName, value) ->
            if(value == -1)
            throw Error("Column $colName could not be found in the spreadsheet")
        }

        var currentTitle = ""
        var currentTestCaseId = ""
        var currentTestCaseLanguage = ""
        var testSteps = mutableListOf<TestStep>()

        fun addTestStep(userInput: String, expectedResult: String) {
            val input = if (userInput.startsWith("[")) ACTION_MAPPINGS.getValue("${userInput}#$currentTestCaseLanguage") else userInput
            if (testSteps.isEmpty()) {
                testSteps = mutableListOf(TestStep(input, expectedResult))
            } else {
                testSteps.add(TestStep(input, expectedResult))
            }
        }

        val scenarios = rows.drop(1).foldIndexed(mutableListOf<TestScenario>()) { index, acc, row ->
            fun getRowElement(colName: String): String {
                return row[cols[colName]!!]
            }

            if (row.isEmpty() || index == rows.size - 2) {
                if (testSteps.isNotEmpty()) {
                    acc.add(
                        createTestScenario(
                            testSteps,
                            range,
                            currentTitle,
                            currentTestCaseId,
                            currentTestCaseLanguage
                        )
                    )
                    testSteps.clear()
                }
            } else {
                if (getRowElement(TEST_CASE_TITLE).isNotEmpty()) {
                    currentTitle = getRowElement(TEST_CASE_TITLE)
                    currentTestCaseId = getRowElement(TEST_CASE_ID)
                    currentTestCaseLanguage = getRowElement(TEST_CASE_LANGUAGE).lowercase()
                    addTestStep(getRowElement(USER_INPUT), getRowElement(EXPECTED_RESULT))
                } else {
                    addTestStep(getRowElement(USER_INPUT), getRowElement(EXPECTED_RESULT))
                    if (index == rows.size - 1) {
                        acc.add(createTestScenario(testSteps, range, currentTitle, currentTestCaseId, currentTestCaseLanguage))
                        testSteps.clear()
                    }
                }
            }

            acc
        }

        println("${scenarios.size} scenarios generated.")

        return scenarios.toList()
    }
}
