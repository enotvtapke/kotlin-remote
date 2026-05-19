plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.5.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val rpcVersion = "0.5.0"
    val ktorVersion = "3.2.1"

    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-server:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-client:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:$rpcVersion")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("ch.qos.logback:logback-classic:1.3.14")
}

kotlin {
    jvmToolchain(21)
}
