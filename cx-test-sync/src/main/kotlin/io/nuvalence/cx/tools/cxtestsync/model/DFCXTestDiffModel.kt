package io.nuvalence.cx.tools.cxtestsync.model

data class DFCXTestDiff (
    val testCaseId: String,
    val testCaseName: String?,
    val tags: List<String>?,
    val notes: String?
)
