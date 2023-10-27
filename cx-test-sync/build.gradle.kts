group = "io.nuvalence.cx-tools"
version = "0.0.1"

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
    implementation(kotlin("reflect"))

    implementation("gov.ny.dol:dol-ui-ccai-domain:0.0.1")
    implementation("io.nuvalence.cx-tools:cx-test-core:0.0.1")

    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.cloud:google-cloud-dialogflow-cx:0.25.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")
}

tasks.register("setup") {
    doLast {
        copy {
            from("src/main/resources/template.properties")
            into("src/main/resources/")
            rename("template.properties", "default.properties")
        }
    }
}

tasks.register<JavaExec>("run") {
    val properties = listOf("spreadsheetId", "credentialsUrl", "agentPath", "dfcxEndpoint")
    systemProperties(project.properties.filter { (key, _) -> key in properties })

    group = "application"
    mainClass.set("io.nuvalence.cx.tools.cxtestsync.Main")
    classpath = sourceSets["main"].runtimeClasspath
}
