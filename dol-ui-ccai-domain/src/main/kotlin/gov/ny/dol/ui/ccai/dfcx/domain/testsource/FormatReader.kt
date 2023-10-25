package gov.ny.dol.ui.ccai.dfcx.domain.testsource

import gov.ny.dol.ui.ccai.dfcx.domain.model.test.TestScenario

interface FormatReader {
    fun read(range: String) : List<TestScenario>
}
