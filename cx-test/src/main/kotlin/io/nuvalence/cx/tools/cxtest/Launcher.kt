package io.nuvalence.cx.tools.cxtest

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
            val clazz = Class.forName("gov.ny.dol.ui.ccai.dfcx.domain.util.Properties")
            val companionField = clazz.getDeclaredField("Companion")
            companionField.isAccessible = true
            val companionInstance = companionField.get(null)
            val method = companionField.type.getDeclaredMethod("init", String::class.java)
            if (args.isEmpty()) {
                method.invoke(companionInstance, "default.properties")
            } else {
                method.invoke(companionInstance, args[0])
            }

            val request: LauncherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines("junit-jupiter"))
                .selectors(selectClass("gov.ny.dol.ui.ccai.dfcx.domain.DFCXTestBuilderSpec"))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
                .filters(TagFilter.includeTags("dfcx"), TagFilter.excludeTags("e2e|smoke"))
                .build()

            val launcher = LauncherFactory.create()

            val summaryListener = SummaryGeneratingListener()
            launcher.registerTestExecutionListeners(summaryListener)

            launcher.execute(request)

            println("Tests found: ${summaryListener.summary.testsFoundCount}")
            println("Tests succeeded: ${summaryListener.summary.testsSucceededCount}")
            println("Tests failed: ${summaryListener.summary.testsFailedCount}")
        }
    }
}
