package io.nuvalence.cx.tools.cxtest.testsource

import io.nuvalence.cx.tools.cxtest.model.test.TestScenario
import io.nuvalence.cx.tools.cxtest.model.test.TestStep
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
        testSteps: List<TestStep>, range: String, section: String, title: String, languageCode: String, sourceLocator: Any?
    ): TestScenario {
        return TestScenario(
            title = "$range - $section - $title - L${sourceLocator}",
            testSteps = testSteps.toList(),
            languageCode = languageCode,
            sourceId = range,
            sourceLocator = sourceLocator
        )
    }

    fun read(range: String, languageCode: String): List<TestScenario> {
        val url = PROPERTIES.CREDENTIALS_URL.get()!!
        val spreadsheetId = PROPERTIES.SPREADSHEET_ID.get()!!
        val rows = SheetReader(
            URL(url), spreadsheetId, range
        ).read()

        var currentSection = ""
        var currentTitle = ""
        var currentLineNumber = 0
        var testSteps = mutableListOf<TestStep>()

        val rowSkip = 2
        val scenarios = rows.drop(1).foldIndexed(mutableListOf<TestScenario>()) { index, acc, row ->
            if (row.size == 1) {
                currentSection = row[0]
            } else if (row.size >= 7) {
                if (row[TEST_CASE_TITLE].isNotEmpty()) {
                    if (testSteps.isNotEmpty() || index == rows.size - rowSkip) {
                        acc.add(createTestScenario(
                            testSteps = testSteps,
                            range = range,
                            section = currentSection,
                            title = currentTitle,
                            languageCode = languageCode,
                            sourceLocator = testSteps[0].sourceLocator))
                        testSteps.clear()
                    }
                    currentTitle = row[TEST_CASE_TITLE]
                    currentLineNumber = index + rowSkip
                    testSteps = mutableListOf(
                        TestStep(
                        input = row[USER_INPUT],
                        expectedResponse = row[EXPECTED_RESULT],
                        sourceLocator = currentLineNumber)
                    )
                } else {
                    testSteps.add(
                        TestStep(
                        input = row[USER_INPUT],
                        expectedResponse = row[EXPECTED_RESULT],
                        sourceLocator = currentLineNumber)
                    )
                    if (index == rows.size - rowSkip) {
                        acc.add(createTestScenario(
                            testSteps = testSteps,
                            range = range,
                            section = currentSection,
                            title = currentTitle,
                            languageCode = languageCode,
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
