package io.nuvalence.cx.tools.cxtest

import io.nuvalence.cx.tools.cxtest.util.Properties
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TagFilter
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.File
import java.net.URL
import java.util.*
import java.util.jar.JarFile

class Launcher {
    companion object {
        private fun findClassesInPackage(packageName: String): List<String> {
            val classNames = mutableListOf<String>()
            val path = packageName.replace('.', '/')
            val resources: Enumeration<URL> = Thread.currentThread().contextClassLoader.getResources(path)

            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                if (resource.protocol.equals("jar", ignoreCase = true)) {
                    val jarPath = resource.path.substring(5, resource.path.indexOf("!"))
                    classNames.addAll(getClassNamesFromJar(jarPath, path))
                } else if (resource.protocol.equals("file", ignoreCase = true)) {
                    classNames.addAll(getClassNamesFromFile(resource.path, path))
                }
            }

            return classNames
        }

        private fun getClassNamesFromJar(jarPath: String, path: String): List<String> {
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

        private fun getClassNamesFromFile(buildDir: String, path: String): List<String> {
            val baseDir = File(buildDir)
            return baseDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .map { it.relativeTo(baseDir).path }
                .map { "$path.$it" }
                .map { it.removeSuffix(".class").replace(File.separatorChar, '.') }
                .toList()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            Properties.init(args[0])

            val includeTags = Properties.INCLUDE_TAGS
            val excludeTags = Properties.EXCLUDE_TAGS
            val testPackage = Properties.TEST_PACKAGE
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
