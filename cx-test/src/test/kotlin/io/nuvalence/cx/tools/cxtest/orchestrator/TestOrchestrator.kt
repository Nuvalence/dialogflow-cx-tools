package io.nuvalence.cx.tools.cxtest.orchestrator

import io.nuvalence.cx.tools.cxtest.model.TestScenario
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES

data class ExecutionPath(val path: List<Int>) {
    operator fun get(index: Int): Int {
        return path[index]
    }
}

enum class TestOrchestrationMode(val value: String) {
    SIMPLE("simple") {
        override fun generateExecutionPaths(testScenarios: List<TestScenario>): Map<TestScenario, List<ExecutionPath>> {
            return testScenarios.associateWith { testScenario -> listOf(ExecutionPath(List(testScenario.testSteps.size) { 0 })) }
        }
    },
    COMPREHENSIVE("comprehensive") {
        override fun generateExecutionPaths(testScenarios: List<TestScenario>): Map<TestScenario, List<ExecutionPath>> {
            return testScenarios.associateWith { testScenario ->
                val modelPath = List(testScenario.testSteps.size) { 0 }
                val counts = testScenario.testSteps.map { testStep ->
                    testStep.input.size
                }

                var isFirstMultimessageNode = true
                val pathList = counts.foldIndexed(mutableListOf<ExecutionPath>()) { index, acc, count ->
                    if (count > 1) {
                        for (i in (if (isFirstMultimessageNode) 0 else 1) until count) {
                            val path = modelPath.toMutableList()
                            path[index] = i
                            acc.add(ExecutionPath(path))
                        }
                        isFirstMultimessageNode = false
                    }
                    acc
                }

                pathList
            }
        }
    };

    abstract fun generateExecutionPaths(testScenarios: List<TestScenario>): Map<TestScenario, List<ExecutionPath>>

    companion object {
        infix fun from(value: String): TestOrchestrationMode =
            TestOrchestrationMode.values().firstOrNull { testOrchestrationMode -> testOrchestrationMode.value == value }
                ?: SIMPLE
    }
}

class OrchestratedTestMap(private val testMap: Map<TestScenario, List<ExecutionPath>>) {
    constructor(testScenarios: List<TestScenario>) : this(
        TestOrchestrationMode.from(PROPERTIES.ORCHESTRATION_MODE.get()).generateExecutionPaths(testScenarios)
    )

    constructor(testScenarios: List<TestScenario>, orchestrationMode: String) : this(
        TestOrchestrationMode.from(orchestrationMode).generateExecutionPaths(testScenarios)
    )

    fun generatePairs(): List<Pair<TestScenario, ExecutionPath>> {
        return testMap.entries.map { (testScenario, executionPaths) ->
            executionPaths.map { executionPath ->
                Pair(testScenario, executionPath)
            }
        }.flatten()
    }
}