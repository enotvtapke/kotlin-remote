plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.kotlin.plugin.remote")
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.serialization)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.logback.classic)

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)

    implementation(libs.ktor.serialization.json)

    implementation(libs.serialization.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.register<JavaExec>("benchPerCall") {
    group = "verification"
    mainClass.set("bench.PerCallBenchKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("benchLocal") {
    group = "verification"
    mainClass.set("bench.LocalDispatchBenchKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("benchLease") {
    group = "verification"
    mainClass.set("bench.LeaseBenchKt")
    classpath = sourceSets["main"].runtimeClasspath
}
