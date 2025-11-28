plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.remote)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.serialization)
}

group = "org.jetbrains.kotlinx"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core"))
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiations)
    implementation(libs.ktor.client.auth)
    
    implementation(libs.logback.classic)

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)

    implementation(libs.ktor.serialization.json)

    implementation(libs.serialization.core)

    testImplementation(libs.ktor.server.test.host)
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