package io.nuvalence.cx.tools.cxtest

import io.nuvalence.cx.tools.cxtestcore.Properties
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.TagFilter
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.net.URL
import java.util.*
import java.util.jar.JarFile

class Launcher {
    companion object {
        fun findClassesInPackage(packageName: String): List<String> {
            val classNames = mutableListOf<String>()
            val path = packageName.replace('.', '/')
            val resources: Enumeration<URL> = Thread.currentThread().contextClassLoader.getResources(path)

            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                if (resource.protocol.equals("jar", ignoreCase = true)) {
                    val jarPath = resource.path.substring(5, resource.path.indexOf("!"))
                    classNames.addAll(getClassNamesFromJar(jarPath, path))
                }
            }

            return classNames
        }

        fun getClassNamesFromJar(jarPath: String, path: String): List<String> {
            val classNames = mutableListOf<String>()

            JarFile(jarPath).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith(path) && entry.name.endsWith(".class")) {
                        val className = entry.name.replace("/", ".").replace(".class", "")
                        classNames.add(className)
                    }
                }
            }

            return classNames
        }

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) {
                Properties.init("default.properties")
            } else {
                Properties.init(args[0])
            }

            val includeTags = Properties.getProperty<String>("includeTags")
            val excludeTags = Properties.getProperty<String>("excludeTags")
            val testPackage = Properties.getProperty<String>("testPackage")
            val classNames = findClassesInPackage(testPackage)

            val requestBuilder: LauncherDiscoveryRequestBuilder = LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines("junit-jupiter"))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
                .filters(TagFilter.includeTags(includeTags), TagFilter.excludeTags(excludeTags))

            classNames.forEach { className ->
                requestBuilder.selectors(selectClass(className))
            }

            val launcher = LauncherFactory.create()

            val summaryListener = SummaryGeneratingListener()
            launcher.registerTestExecutionListeners(summaryListener)

            launcher.execute(requestBuilder.build())

            println("Tests found: ${summaryListener.summary.testsFoundCount}")
            println("Tests succeeded: ${summaryListener.summary.testsSucceededCount}")
            println("Tests failed: ${summaryListener.summary.testsFailedCount}")
        }
    }
}
