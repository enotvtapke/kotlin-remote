plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "kotlin-remote"

includeBuild("gradle-plugin")
includeBuild("compiler-plugin")
include("integration-tests")
include("compiler-plugin-tests")