plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    application
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":cx-shared"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    implementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    implementation("org.junit.platform:junit-platform-launcher:1.10.0")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.cloud:google-cloud-dialogflow-cx:0.25.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")

    implementation("me.xdrop:fuzzywuzzy:1.2.0")
}

val reportDestinationPath = "$buildDir/reports/tests"

tasks.register<JavaExec>("runTests") {
    val properties = listOf("agentPath", "spreadsheetId", "dfcxTagFilter", "credentialsUrl", "orchestrationMode", "matchingMode", "matchingRatio", "dfcxEndpoint")
    systemProperties(project.properties.filter { (key, _) -> key in properties })

    group = "application"
    mainClass.set("io.nuvalence.cx.tools.cxtest.Launcher")
    classpath = sourceSets["main"].runtimeClasspath
    //args = listOf("arg1", "arg2") // If you have any arguments

    outputs.upToDateWhen { false }
}
