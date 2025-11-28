plugins {
    `kotlin-dsl`
    kotlin("jvm") version ("2.0.21")
}

group = "org.jetbrains.kotlinx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
}

gradlePlugin {
    plugins {
        create("plugin") {
            id = "org.jetbrains.kotlinx.kremote.plugin"

            implementationClass = "kotlinx.kremote.KRemoteGradlePlugin"
        }
    }
}
