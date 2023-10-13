package io.nuvalence.cx.tools.cxtestsync.model

class DFCXTestSpreadsheetModel {
    companion object {
        const val sheetTitle = "Test Results"

        const val TEST_CASE_NAME = "Test Case Name"
        const val TEST_CASE_ID = "Test Case ID"
        const val TAGS = "Tags"
        const val NOTES = "Notes"
        const val USER_INPUT = "User Input"
        const val AGENT_OUTPUT = "Agent Output"
        const val STATUS = "Status"
        const val ERROR_DETAILS = "Error Details"

        val colNames = listOf(TEST_CASE_NAME, TEST_CASE_ID, TAGS, NOTES, USER_INPUT, AGENT_OUTPUT, STATUS, ERROR_DETAILS)
    }
}
