plugins {
    kotlin("jvm") version ("2.2.0")
}

repositories {
    mavenCentral()
}

group = "org.jetbrains.kotlinx"
version = "1.0-SNAPSHOT"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.0")
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }

    compilerOptions {
        freeCompilerArgs.set(listOf("-Xcontext-parameters"))
    }
}