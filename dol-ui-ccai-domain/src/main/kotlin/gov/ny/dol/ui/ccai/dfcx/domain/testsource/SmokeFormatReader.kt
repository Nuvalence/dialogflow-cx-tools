package gov.ny.dol.ui.ccai.dfcx.domain.testsource

import gov.ny.dol.ui.ccai.dfcx.domain.model.test.TestScenario
import gov.ny.dol.ui.ccai.dfcx.domain.model.test.TestStep
import gov.ny.dol.ui.ccai.dfcx.domain.util.ACTION_MAPPINGS
import gov.ny.dol.ui.ccai.dfcx.domain.util.LANGUAGE_MAPPINGS
import io.nuvalence.cx.tools.cxtestcore.Properties
import io.nuvalence.cx.tools.shared.SheetReader
import java.net.URL

class SmokeFormatReader : FormatReader {
    companion object {
        const val TEST_CASE_ID = "Test Case ID"
        const val TEST_CASE_LANGUAGE = "Language"
        const val TEST_CASE_TITLE = "Test Description"
        const val USER_INPUT = "Test Steps / User Input"
        const val EXPECTED_RESULT = "Expected Telephony Result"
        const val STEP_STATUS = "Step Status"
        const val COMMENTS = "Comments"

        val url = Properties.getProperty<URL>("credentialsUrl")
        val spreadsheetId = Properties.getProperty<String>("spreadsheetId")

        val colNames = listOf(TEST_CASE_ID, TEST_CASE_LANGUAGE, TEST_CASE_TITLE, USER_INPUT, EXPECTED_RESULT, STEP_STATUS, COMMENTS)
        lateinit var cols : Map<String, Int>
    }

    private fun createTestScenario(
        testSteps: List<TestStep>, range: String, title: String, testCaseId: String, testCaseLanguage: String, sourceLocator: Any?
    ): TestScenario {
        return TestScenario(
            title = "$range - $title - $testCaseId",
            testSteps = testSteps.toList(),
            languageCode = LANGUAGE_MAPPINGS.getValue(testCaseLanguage),
            sourceId = range,
            sourceLocator = sourceLocator)
    }

    fun listSheets(prefix: String): List<String> {
        return SheetReader(url, spreadsheetId, "").listSheets().filter { sheetName -> sheetName.startsWith(prefix) }
    }

    override fun read(range: String): List<TestScenario> {
        val rows = SheetReader(
            url, spreadsheetId, range
        ).read()

        val headerRow = rows[0].map { item -> item.trim() }
        cols = colNames.associateWith { colName -> headerRow.indexOf(colName) }
        cols.forEach { (colName, value) ->
            if(value == -1)
            throw Error("Column $colName could not be found in the spreadsheet")
        }

        var currentTitle = ""
        var currentTestCaseId = ""
        var currentTestCaseLanguage = ""
        var currentLineNumber = 0
        var testSteps = mutableListOf<TestStep>()

        fun addTestStep(userInput: String, expectedResult: String, sourceLocator: Any?) {
            val input = if (userInput.startsWith("[")) ACTION_MAPPINGS.getValue("${userInput}#$currentTestCaseLanguage") else userInput
            if (testSteps.isEmpty()) {
                testSteps = mutableListOf(TestStep(input, expectedResult, sourceLocator))
            } else {
                testSteps.add(TestStep(input, expectedResult, sourceLocator))
            }
        }

        val rowSkip = 2
        val scenarios = rows.drop(1).foldIndexed(mutableListOf<TestScenario>()) { index, acc, row ->
            currentLineNumber = index + rowSkip
            fun getRowElement(colName: String): String {
                return row[cols[colName]!!]
            }

            if (row.isEmpty() || index == rows.size - rowSkip) {
                if (testSteps.isNotEmpty()) {
                    acc.add(
                        createTestScenario(
                            testSteps = testSteps,
                            range = range,
                            title = currentTitle,
                            testCaseId = currentTestCaseId,
                            testCaseLanguage = currentTestCaseLanguage,
                            sourceLocator = testSteps[0].sourceLocator
                        )
                    )
                    testSteps.clear()
                }
            } else {
                if (getRowElement(TEST_CASE_TITLE).isNotEmpty()) {
                    currentTitle = getRowElement(TEST_CASE_TITLE)
                    currentTestCaseId = getRowElement(TEST_CASE_ID)
                    currentTestCaseLanguage = getRowElement(TEST_CASE_LANGUAGE).lowercase()
                    addTestStep(
                        userInput = getRowElement(USER_INPUT),
                        expectedResult = getRowElement(EXPECTED_RESULT),
                        sourceLocator = currentLineNumber
                    )
                } else {
                    addTestStep(
                        userInput = getRowElement(USER_INPUT),
                        expectedResult = getRowElement(EXPECTED_RESULT),
                        sourceLocator = currentLineNumber
                    )
                    if (index == rows.size - rowSkip + 1) {
                        acc.add(createTestScenario(
                            testSteps = testSteps,
                            range = range,
                            title = currentTitle,
                            testCaseId = currentTestCaseId,
                            testCaseLanguage = currentTestCaseLanguage,
                            sourceLocator = testSteps[0].sourceLocator))
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
