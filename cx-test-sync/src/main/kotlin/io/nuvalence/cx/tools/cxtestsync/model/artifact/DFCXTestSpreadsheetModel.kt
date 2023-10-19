package io.nuvalence.cx.tools.cxtestsync.model.artifact

class DFCXTestSpreadsheetModel {
    companion object {
        const val sheetTitle = "Test Results"

        const val TEST_CASE_ID = "Test Case ID"
        const val TEST_CASE_NAME = "Test Case Name"
        const val TAGS = "Tags"
        const val NOTES = "Notes"
        const val USER_INPUT = "User Input"
        const val AGENT_OUTPUT = "Agent Output"
        const val TEST_RESULT = "Test Result"
        const val TEST_RESULT_DETAILS = "Test Result Details"
        const val TEST_PAYLOADS = "Test Payloads"
        const val TEST_SSN = "Test SSN"
        const val COMMENTS = "Comments"

        val colNames = listOf(TEST_CASE_NAME, TEST_CASE_ID, TAGS, NOTES, USER_INPUT, AGENT_OUTPUT, TEST_RESULT, TEST_RESULT_DETAILS, TEST_PAYLOADS, TEST_SSN, COMMENTS)
    }
}
