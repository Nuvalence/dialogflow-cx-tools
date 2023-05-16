plugins {
    `java-library`
    kotlin("jvm")  version "1.7.10"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
dependencies {
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")
}