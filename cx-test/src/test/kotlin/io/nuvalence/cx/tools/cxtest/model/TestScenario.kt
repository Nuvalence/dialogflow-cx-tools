package io.nuvalence.cx.tools.cxtest.model

data class TestStep(
    val input: List<String>, val expectedResponse: String
) {
    constructor(input: String, expectedResponse: String) : this(input.split("\n"), expectedResponse)
}

data class TestScenario(
    val title: String, val testSteps: List<TestStep>, val languageCode: String
)
