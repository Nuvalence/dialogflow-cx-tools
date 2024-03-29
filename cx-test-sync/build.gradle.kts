plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
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


application {
    mainClass.set("io.nuvalence.cx.tools.cxtestsync.Main")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    mergeServiceFiles()
}

