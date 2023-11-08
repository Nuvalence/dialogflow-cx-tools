package io.nuvalence.cx.tools.cxtest.model.test

data class TestStep(
    val input: List<String>, val expectedResponse: String, val sourceLocator: Any?
) {
    constructor(input: String, expectedResponse: String, sourceLocator: Any?) : this(input.split("\n"), expectedResponse, sourceLocator)
}

data class TestScenario(
    val title: String, val testSteps: List<TestStep>, val languageCode: String, val sourceId: String, val sourceLocator: Any?
) {
    override fun toString() : String {
        return title
    }
}
