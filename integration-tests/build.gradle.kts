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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}