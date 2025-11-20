import kotlinx.kremote.TestCompilerExtension

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kremote.plugin")
}

configure<TestCompilerExtension> {
    enabled = true
}

group = "org.jetbrains.kotlinx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xphases-to-dump-after=ALL")
        freeCompilerArgs.add("-Xdump-directory=/home/enotvtapke/work/kotlin-remote/dump-ir")
    }
}