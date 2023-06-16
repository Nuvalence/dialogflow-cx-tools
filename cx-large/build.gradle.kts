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
}


application {
    mainClass.set("io.nuvalence.cx.tools.large.MainKt")
}
