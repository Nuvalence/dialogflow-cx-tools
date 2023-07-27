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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    testImplementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    testImplementation("com.google.cloud:google-cloud-dialogflow-cx:0.25.0")
    testImplementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")

    testImplementation("me.xdrop:fuzzywuzzy:1.2.0")
}

tasks.test {
    // Leave these findProperty calls as is -- Kotlin's type checking does not play nice with Gradle's API and causes false positives upon linting.
    systemProperty("agentPath", project.findProperty("agentPath"))
    systemProperty("spreadsheetId", project.findProperty("spreadsheetId"))
    systemProperty("credentialsUrl", project.findProperty("credentialsUrl"))
    systemProperty("orchestrationMode", project.findProperty("orchestrationMode"))
    systemProperty("matchingMode", project.findProperty("matchingMode"))
    systemProperty("matchingRatio", project.findProperty("matchingRatio"))
    systemProperty("dfcxEndpoint", project.findProperty("dfcxEndpoint"))

    useJUnitPlatform {
        // Enable parallel test execution
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")

        // Set the parallelism factor (optional)
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")


        val includeTagsProperty = project.findProperty("includeTags")?.toString()
        val excludeTagsProperty = project.findProperty("excludeTags")?.toString()
        if (includeTagsProperty?.isNotBlank() == true) {
            println("Including tags $includeTagsProperty")
            includeTags(includeTagsProperty)
        } else {
            println("Defaulting to both e2e and smoke")
            includeTags("e2e|smoke")
        }

        if (excludeTagsProperty?.isNotBlank() == true) {
            excludeTags(excludeTagsProperty)
        }
    }
    finalizedBy("aggregateTestResults")
    outputs.upToDateWhen { false }
}


task("aggregateTestResults", type = TestReport::class) {
    // Set the output directory for the test report
    destinationDir = file("$buildDir/reports/tests")

    // Set the test results directory
    reportOn(tasks.withType(Test::class))
}
