package io.nuvalence.cx.tools.cxtestsync.model.diff

class DFCXTestDiff (
    val testCaseId: String,
    val testCaseName: String?,
    val tags: List<String>?,
    val notes: String?
) {
    override fun toString() : String {
        val testCaseIdFragment = "Test Case ID: $testCaseId"
        val testCaseNameFragment = if (testCaseName != null) "\n  Test Case Name: $testCaseName" else ""
        val tagsFragment = if (tags != null) "\n  Tags: $tags" else ""
        val notesFragment = if (notes != null) "\n  Notes: $notes" else ""

        return listOf(testCaseIdFragment, testCaseNameFragment, tagsFragment, notesFragment).joinToString("")
    }
}
