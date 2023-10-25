plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("maven-publish")
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

    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.cloud:google-cloud-dialogflow-cx:0.25.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")

    implementation("me.xdrop:fuzzywuzzy:1.2.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()  // Publish to the local Maven repository
    }
}
