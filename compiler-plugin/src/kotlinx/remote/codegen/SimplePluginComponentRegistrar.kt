package kotlinx.remote.codegen

import kotlinx.remote.codegen.backend.RemoteIrExtension
import kotlinx.remote.codegen.backend.noarg.NoArgIrGenerationExtension
import kotlinx.remote.codegen.common.RemoteClassId
import kotlinx.remote.codegen.lineReport.LineReportExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class SimplePluginComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val lineReportPath = LineReportExtension.resolveOutputPath(
            optionPath = configuration.get(LINE_REPORT_OUTPUT_KEY),
        )

        if (lineReportPath != null) {
            // Counter-only mode: register *only* the line-report extension and skip all
            // code-generating transformations. This keeps the plugin tolerant of projects
            // that do not depend on kotlinx-remote at all (e.g. Kotlin RPC projects we want
            // to measure side-by-side).
            val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME)
            IrGenerationExtension.registerExtension(LineReportExtension(lineReportPath, moduleName))
            return
        }

        FirExtensionRegistrarAdapter.registerExtension(SimplePluginRegistrar())
        IrGenerationExtension.registerExtension(
            NoArgIrGenerationExtension(
                listOf(RemoteClassId.remoteSerializableAnnotation.asSingleFqName().asString()),
                false,
            )
        )
        IrGenerationExtension.registerExtension(RemoteIrExtension())
    }
}
