plugins {
    `kotlin-dsl`
    kotlin("jvm") version ("2.2.0")
}

group = "de.jensklingenberg"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.2.0")
}

gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "compiler.gradleplugin.helloworld"
            implementationClass = "de.jensklingenberg.gradle.HelloWorldGradleSubPlugin"
        }
    }
}
