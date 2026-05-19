package org.jetbrains.kotlin.compiler.plugin.template

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.plugin.remote.BuildConfig
import org.jetbrains.kotlin.plugin.remote.BuildConfig.CORE_CLASSES_LIBRARY_COORDINATES
import org.jetbrains.kotlin.plugin.remote.BuildConfig.CORE_KTOR_LIBRARY_COORDINATES
import org.jetbrains.kotlin.plugin.remote.BuildConfig.CORE_LIBRARY_COORDINATES

@Suppress("unused") // Used via reflection.
class SimpleGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("simplePlugin", SimpleGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // The plugin is always considered applicable so that the user gets predictable
        // behaviour from a regular `id("org.jetbrains.kotlin.plugin.remote")` apply. The
        // line-report path is consulted in `applyToCompilation` to decide whether the
        // plugin should fall back to a measurement-only mode that skips transformations.
        return true
    }

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(SimpleGradleExtension::class.java)

        // The runtime is injected by default. Projects that want to apply the plugin
        // *only* to be measured (e.g. Kotlin RPC modules in the integration-tests build)
        // can opt out via `simplePlugin { skipRuntimeInjection = true }`; the line-report
        // mode does not by itself disable injection because Kotlin Remote projects still
        // need kotlinx-remote types resolved on the classpath in order to compile.
        val skipInjection = extension.skipRuntimeInjection.getOrElse(false)

        if (!skipInjection) {
            kotlinCompilation.dependencies {
                implementation(CORE_LIBRARY_COORDINATES)
                implementation(CORE_CLASSES_LIBRARY_COORDINATES)
                implementation(CORE_KTOR_LIBRARY_COORDINATES)
            }
            if (kotlinCompilation.implementationConfigurationName == "metadataCompilationImplementation") {
                project.dependencies.add("commonMainImplementation", CORE_LIBRARY_COORDINATES)
                project.dependencies.add("commonMainImplementation", CORE_CLASSES_LIBRARY_COORDINATES)
                project.dependencies.add("commonMainImplementation", CORE_KTOR_LIBRARY_COORDINATES)
            }
        }

        return project.provider {
            // Two ways to enable the line-report mode (resolved lazily so the
            // path can be set after configuration time, e.g. by `taskGraph.whenReady`):
            //  1. set `simplePlugin.lineReport` in the build script;
            //  2. pass `-PkotlinxRemote.lineReport=<path>` on the gradle command line.
            val cliPath = (project.findProperty(LINE_REPORT_PROPERTY) as? String)?.takeIf { it.isNotBlank() }
            val configuredPath = extension.lineReport.orNull?.asFile?.absolutePath
            val lineReportPath = cliPath ?: configuredPath

            buildList {
                if (lineReportPath != null) {
                    add(SubpluginOption(key = "lineReport", value = lineReportPath))
                }
            }
        }
    }

    companion object {
        const val LINE_REPORT_PROPERTY: String = "kotlinxRemote.lineReport"
    }
}
