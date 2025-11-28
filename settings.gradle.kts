rootProject.name = "kotlin-remote"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.google.com")
        maven("https://plugins.gradle.org/m2/")
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

includeBuild("gradle-plugin")
includeBuild("compiler-plugin")
include("integration-tests")
include("compiler-plugin-tests")
include("core")