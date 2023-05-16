val kotlin_version: String by project

buildscript {
    dependencies {
        classpath("gradle.plugin.com.google.cloud.artifactregistry:artifactregistry-gradle-plugin:2.2.0")
    }
}

plugins {
    id("java-library")
    kotlin("jvm") version "1.7.10"
    application
}

tasks.matching { it.name.startsWith("publish") && !it.name.startsWith("publishToMavenLocal") }.all {
    dependsOn("release")
}
