import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":cx-shared"))
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    testImplementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    testImplementation("com.google.cloud:google-cloud-dialogflow-cx:0.25.0")
    testImplementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")

    testImplementation("me.xdrop:fuzzywuzzy:1.2.0")
}

val reportDestinationPath = "$buildDir/reports/tests"

val aggregateTestResults by tasks.register<TestReport>("aggregateTestResults") {
    destinationDir = file(reportDestinationPath)

    // Set the test results directory
    reportOn(tasks.withType(Test::class))
}

val postProcessTestReport = tasks.register<DefaultTask>("postProcessTestReport") {
    doLast {
        file(reportDestinationPath).walkTopDown().forEach { file ->
            if (file.isFile) {
                val content = file.readText()
                val stacktraceRegex = Regex("[\\s]+at.*\\..*:.*\\)\n")
                val locationRegex = Regex("&quot; ==&gt;.*\\)")
                val modifiedContent = content.replace(stacktraceRegex, "").replace(locationRegex, "\"")
                file.bufferedWriter().use { writer ->
                    writer.write(modifiedContent)
                }
            }
        }
    }
}

val testTask = tasks.withType<Test> {
    val properties = listOf("agentPath", "spreadsheetId", "credentialsUrl", "orchestrationMode", "matchingMode", "matchingRatio", "dfcxEndpoint")
    systemProperties(project.properties.filter { (key, _) -> key in properties })

    useJUnitPlatform {
        // Enable parallel test execution
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")

        // Set the parallelism factor (optional)
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")

        systemProperty("junit.jupiter.listeners", "io.nuvalence.cx.tools.cxtest.listener.DynamicTestListener")

        val includeTagsProperty = project.findProperty("includeTags")?.toString()
        val excludeTagsProperty = project.findProperty("excludeTags")?.toString()
        if (includeTagsProperty?.isNotBlank() == true) {
            println("Including tags $includeTagsProperty")
            includeTags(includeTagsProperty)
        } else {
            println("Defaulting to dfcx")
            includeTags("dfcx")
        }

        if (excludeTagsProperty?.isNotBlank() == true) {
            excludeTags(excludeTagsProperty)
        }
    }

    finalizedBy(aggregateTestResults, postProcessTestReport)

    outputs.upToDateWhen { false }
}
