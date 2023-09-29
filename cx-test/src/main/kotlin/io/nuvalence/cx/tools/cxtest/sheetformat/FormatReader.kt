package io.nuvalence.cx.tools.cxtest.sheetformat

import io.nuvalence.cx.tools.cxtest.model.TestScenario

interface FormatReader {
    fun read(range: String) : List<TestScenario>
}
