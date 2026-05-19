package kotlinx.remote.codegen

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.plugin.remote.BuildConfig

internal val LINE_REPORT_OUTPUT_KEY: CompilerConfigurationKey<String> =
    CompilerConfigurationKey.create("kotlinx-remote.lineReport.output")

@Suppress("unused") // Used via reflection.
class SimpleCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = BuildConfig.KOTLIN_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = OPTION_LINE_REPORT,
            valueDescription = "<path-to-file-or-dir>",
            description = "Write a JSON line-count report and skip code transformations. " +
                "If the path ends with .json it is used as is; otherwise it is treated as a " +
                "directory and one file per module is written under it.",
            required = false,
            allowMultipleOccurrences = false,
        ),
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            OPTION_LINE_REPORT -> configuration.put(LINE_REPORT_OUTPUT_KEY, value)
            else -> error("Unexpected config option: '${option.optionName}'")
        }
    }

    companion object {
        const val OPTION_LINE_REPORT: String = "lineReport"
    }
}
