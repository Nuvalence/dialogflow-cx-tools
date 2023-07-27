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
        const val TEST_CASE_ID = 1
        const val TEST_CASE_LANGUAGE = 2
        const val TEST_CASE_TITLE = 4
        const val USER_INPUT = 5
        const val EXPECTED_RESULT = 6
    }

    private fun createTestScenario(
        testSteps: List<TestStep>, range: String, title: String, testCaseId: String, testCaseLanguage: String
    ): TestScenario {
        return TestScenario("$range - $title - $testCaseId", testSteps.toList(), LANGUAGE_MAPPINGS.getValue(testCaseLanguage))
    }

    fun read(range: String): List<TestScenario> {
        val url = PROPERTIES.CREDENTIALS_URL.get()
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()
        val rows = SheetReader(
            URL(url), spreadsheetId, range
        ).read()

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
                if (row[TEST_CASE_TITLE].isNotEmpty()) {
                    currentTitle = row[TEST_CASE_TITLE]
                    currentTestCaseId = row[TEST_CASE_ID]
                    currentTestCaseLanguage = row[TEST_CASE_LANGUAGE].lowercase()
                    addTestStep(row[USER_INPUT], row[EXPECTED_RESULT])
                } else {
                    addTestStep(row[USER_INPUT], row[EXPECTED_RESULT])
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
