plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.10.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
    val rpcVersion = "0.10.1"
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

kotlin { jvmToolchain(21) }
