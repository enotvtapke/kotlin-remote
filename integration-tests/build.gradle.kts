import java.io.File

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.kotlin.plugin.remote")
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.serialization)
}

group = "org.example"
version = "1.0-SNAPSHOT"

// -----------------------------------------------------------------------------
// Line-report aggregation task. Runs the kotlinx-remote line-report compiler
// pass on a pre-defined set of modules and prints a side-by-side comparison
// table of framework-specific source lines per module (Kotlin Remote vs.
// Kotlin RPC). The report files themselves are written under
// `<integration-tests>/build/line-reports/<module>.json`.
//
// Usage:
//     ./gradlew lineReports
// or, equivalently:
//     ./gradlew compileKotlin -PkotlinxRemote.lineReport=<dir>
// -----------------------------------------------------------------------------
val lineReportDir: Provider<Directory> = layout.buildDirectory.dir("line-reports")

val lineReportComparisons: List<Pair<String, String?>> = listOf(
    ":todoapp" to ":todoapp-kotlinrpc",
    ":social" to ":social-kotlinrpc",
    ":cms" to ":cms-kotlinrpc",
)

// When the `lineReports` task is on the command line we set the global
// project property `kotlinxRemote.lineReport` to a directory; the gradle
// plugin reads this lazily inside its SubpluginOption provider, so by the
// time the kotlin compile tasks query their options the property is already
// populated. Setting it here once means we don't require the user to also
// pass `-P...` themselves.
val lineReportRequested: Boolean =
    gradle.startParameter.taskNames.any { it == "lineReports" || it.endsWith(":lineReports") }

if (lineReportRequested) {
    val absPath = lineReportDir.get().asFile.absolutePath
    rootProject.allprojects.forEach { sub ->
        sub.extensions.extraProperties["kotlinxRemote.lineReport"] = absPath
    }
}

val lineReports by tasks.registering {
    group = "verification"
    description = "Compiles every measured module with the line-report compiler pass enabled."

    val modules = lineReportComparisons.flatMap { (a, b) -> listOfNotNull(a, b) }
    modules.forEach { dependsOn("$it:compileKotlin") }

    // Always re-run so the user gets a fresh table even when the underlying
    // compile tasks are UP-TO-DATE. The report files themselves survive across
    // runs because compile is idempotent for a given source tree.
    outputs.upToDateWhen { false }

    doLast {
        val outDir = lineReportDir.get().asFile
        val rows = lineReportComparisons.map { (kr, krpc) ->
            val krCount = readTotal(File(outDir, "${kr.removePrefix(":")}.json"))
            val krpcCount = krpc?.let { readTotal(File(outDir, "${it.removePrefix(":")}.json")) }
            Triple(kr.removePrefix(":"), krCount, krpcCount)
        }
        val nameLabel = "Application"
        val nameWidth = maxOf(nameLabel.length, rows.maxOf { it.first.length })
        val header = "%-${nameWidth}s  %14s  %12s  %12s".format(
            nameLabel, "Kotlin Remote", "Kotlin RPC", "Δ (KR-krpc)"
        )
        println()
        println(header)
        println("-".repeat(header.length))
        for ((module, kr, krpc) in rows) {
            val delta = if (kr != null && krpc != null) (kr - krpc).toString() else "n/a"
            println(
                "%-${nameWidth}s  %14s  %12s  %12s".format(
                    module,
                    kr?.toString() ?: "n/a",
                    krpc?.toString() ?: "n/a",
                    delta,
                )
            )
        }
        println()
        println("Reports: ${outDir.absolutePath}")
    }
}

fun readTotal(file: File): Int? {
    if (!file.isFile) return null
    val text = file.readText()
    val match = Regex("\"totalFrameworkLines\"\\s*:\\s*(\\d+)").find(text) ?: return null
    return match.groupValues[1].toIntOrNull()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.auth)

    implementation(libs.logback.classic)

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.status.pages)

    implementation(libs.ktor.serialization.json)

    implementation(libs.serialization.core)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}