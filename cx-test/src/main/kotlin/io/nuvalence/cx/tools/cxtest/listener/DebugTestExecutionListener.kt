package io.nuvalence.cx.tools.cxtest.listener

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan

class DebugTestExecutionListener : TestExecutionListener {

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        println("Test Plan Execution Started")
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        println("Test Plan Execution Finished")
    }

    override fun dynamicTestRegistered(testIdentifier: TestIdentifier) {
        println("Dynamic Test Registered: ${testIdentifier.displayName}")
    }

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String) {
        println("Test Skipped: ${testIdentifier.displayName}. Reason: $reason")
    }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        println("Execution Started: ${testIdentifier.displayName}")
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.status == TestExecutionResult.Status.FAILED) {
            testExecutionResult.throwable.ifPresent { throwable ->
                println("Exception encountered:")
                throwable.printStackTrace()
            }
        }
    }

}
