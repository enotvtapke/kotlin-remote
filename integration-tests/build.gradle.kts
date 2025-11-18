plugins {
    kotlin("jvm")
    id("compiler.gradleplugin.helloworld")
}

apply(plugin = "compiler.gradleplugin.helloworld")

configure<de.jensklingenberg.gradle.TestCompilerExtension> {
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