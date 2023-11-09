package io.nuvalence.cx.tools.cxtest.model.artifact

import com.google.api.services.sheets.v4.model.Color

enum class DFCXTestBuilderSummaryArtifactFormat (val categoryName: String, val labelWidth: Int, val dataWidth: Int, val dataPoints: List<SummaryData>) {
    AGENT_SUMMARY("Agent Summary", 200, 500, listOf(
        SummaryData("Agent Path"),
        SummaryData("Agent Name")
    )),
    EXECUTION_INFORMATION("Execution Information", 200, 500, listOf(
        SummaryData("Test Timestamp"),
        SummaryData("Tags Included"),
        SummaryData("Tags Excluded")
    )),
    TEST_SUMMARY("Test Summary", 200, 500, listOf(
        SummaryData("Tests Run"),
        SummaryData("Tests Passed", Color().setRed(0.8f).setGreen(1.0f).setBlue(0.8f)),
        SummaryData("Tests Failed", Color().setRed(1.0f).setGreen(0.8f).setBlue(0.8f)),
    )),
    COVERAGE_SUMMARY("Coverage Summary", 200, 500, listOf(
        SummaryData("Transitions"),
        SummaryData("Intents"),
        SummaryData("Route Groups")
    ));

    data class SummaryData(val labelName: String, val highlightColor: Color? = null)
}

typealias SummaryArtifactFormat = DFCXTestBuilderSummaryArtifactFormat
typealias SummaryData = DFCXTestBuilderSummaryArtifactFormat.SummaryData
