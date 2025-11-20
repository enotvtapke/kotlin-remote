import kotlinx.kremote.TestCompilerExtension

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kremote.plugin")
    id("io.ktor.plugin") version "3.2.1"
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
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation(project(":"))

    testImplementation("io.ktor:ktor-server-test-host:3.2.1")
    implementation("io.ktor:ktor-client-cio")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}