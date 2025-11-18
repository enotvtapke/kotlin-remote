includeBuild("gradle-plugin") {
    dependencySubstitution {
        substitute(module("de.jensklingenberg:gradle-plugin:1.0.0")).using(project(":"))
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "kotlin-remote"

includeBuild("compiler-plugin")
include("integration-tests")