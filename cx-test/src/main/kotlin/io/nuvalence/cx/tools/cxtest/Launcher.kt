package io.nuvalence.cx.tools.cxtest

import io.nuvalence.cx.tools.cxtest.listener.DebugTestExecutionListener
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TagFilter
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val request: LauncherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines("junit-jupiter"))
                .selectors(selectClass(SmokeSpec::class.java))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
                .filters(TagFilter.includeTags("smoke"), TagFilter.excludeTags("e2e"))
                .build()

            val launcher = LauncherFactory.create()

            // Register a listener of your choice
            val debugListener = DebugTestExecutionListener()
            val summaryListener = SummaryGeneratingListener()
            launcher.registerTestExecutionListeners(debugListener, summaryListener)

            launcher.execute(request)


            println("Tests found: ${summaryListener.summary.testsFoundCount}")
            println("Tests succeeded: ${summaryListener.summary.testsSucceededCount}")
            println("Tests failed: ${summaryListener.summary.testsFailedCount}")
        }
    }
}