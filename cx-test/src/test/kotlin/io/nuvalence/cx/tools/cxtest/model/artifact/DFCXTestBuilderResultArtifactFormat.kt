package io.nuvalence.cx.tools.cxtest.model.artifact

enum class DFCXTestBuilderResultArtifactFormat(val headerName: String, val width: Int, val wrapStrategy: String, val isMetadata: Boolean = false) {
    TEST_CASE_ID("Test Case ID", 150, "WRAP", true),
    TEST_CASE_NAME("Test Case Name", 100, "WRAP", true),
    TAGS("Tags", 100, "OVERFLOW_CELL", true),
    NOTES("Notes", 200, "WRAP"),
    USER_INPUT("User Input", 400, "WRAP"),
    AGENT_OUTPUT("Agent Output", 400, "WRAP"),
    TEST_RESULT("Test Result", 100, "WRAP"),
    TEST_RESULT_DETAILS("Test Result Details", 300, "WRAP"),
    TEST_PAYLOADS("Test Payloads", 300, "WRAP"),
    COMMENTS("Comments", 300, "WRAP");

    data class ResultDetails(val message: String, val row: Int, val column: Int)
}

typealias ArtifactFormat = DFCXTestBuilderResultArtifactFormat
typealias ResultDetails = DFCXTestBuilderResultArtifactFormat.ResultDetails