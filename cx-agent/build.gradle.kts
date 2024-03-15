plugins {
    application
    kotlin("jvm")  version "1.7.10"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation(project(mapOf("path" to ":cx-shared")))
    implementation("org.freemarker:freemarker:2.3.32")
    implementation("com.typesafe:config:1.4.2")
    implementation("io.github.config4k:config4k:0.5.0")
    implementation("com.google.cloud:google-cloud-dialogflow-cx:0.48.0")
}

application {
    mainClass.set("io.nuvalence.cx.tools.cxagent.MainKt")
}
