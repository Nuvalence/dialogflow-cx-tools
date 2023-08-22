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
    implementation("io.github.config4k:config4k:0.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}


application {
    mainClass.set("io.nuvalence.cx.tools.phrases.MainKt")
}
