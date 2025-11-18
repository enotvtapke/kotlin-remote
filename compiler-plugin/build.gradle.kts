plugins {
    kotlin("jvm") version ("2.2.0")
}

repositories {
    mavenCentral()
}

group = "de.jensklingenberg"
version = "0.0.1"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.0")
}
