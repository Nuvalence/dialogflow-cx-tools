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
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.aallam.openai:openai-client:3.2.5")
    implementation("io.ktor:ktor-client-cio:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}


application {
    mainClass.set("io.nuvalence.cx.tools.variations.MainKt")
}
