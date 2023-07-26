package io.nuvalence.cx.tools.cxtest.sheetformat

import io.nuvalence.cx.tools.cxtest.model.TestScenario
import io.nuvalence.cx.tools.cxtest.model.TestStep
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.shared.SheetReader
import java.net.URL

class E2EFormatReader {
    companion object {
        const val TEST_CASE_TITLE = 3
        const val USER_INPUT = 5
        const val EXPECTED_RESULT = 6
    }

    private fun createTestScenario(
        testSteps: List<TestStep>, range: String, section: String, title: String, lineNumber: Int, languageCode: String
    ): TestScenario {
        return TestScenario("$range - $section - $title - L${lineNumber}", testSteps.toList(), languageCode)
    }

    fun read(range: String, languageCode: String): List<TestScenario> {
        val url = PROPERTIES.CREDENTIALS_URL.get()
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()
        val rows = SheetReader(
            URL(url), spreadsheetId, range
        ).read()

        var currentSection = ""
        var currentTitle = ""
        var currentLineNumber = 0
        var testSteps = mutableListOf<TestStep>()

        val scenarios = rows.drop(1).foldIndexed(mutableListOf<TestScenario>()) { index, acc, row ->
            if (row.size == 1) {
                currentSection = row[0]
            } else if (row.size >= 7) {
                if (row[TEST_CASE_TITLE].isNotEmpty()) {
                    if (testSteps.isNotEmpty() || index == rows.size - 2) {
                        acc.add(createTestScenario(testSteps, range, currentSection, currentTitle, currentLineNumber, languageCode))
                        testSteps.clear()
                    }
                    currentTitle = row[TEST_CASE_TITLE]
                    currentLineNumber = index + 2 // +1 skipped header, +1 1-indexed rows on sheet
                    testSteps = mutableListOf(TestStep(row[USER_INPUT], row[EXPECTED_RESULT]))
                } else {
                    testSteps.add(TestStep(row[USER_INPUT], row[EXPECTED_RESULT]))
                    if (index == rows.size - 2) {
                        acc.add(createTestScenario(testSteps, range, currentSection, currentTitle, currentLineNumber, languageCode))
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
