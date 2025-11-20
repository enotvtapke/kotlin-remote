package kotlinx.kremote.codegen

import de.jensklingenberg.DebugLogger
import de.jensklingenberg.transform.ExampleIrGenerationExtension
import kotlinx.kremote.codegen.backend.RpcIrExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class KRemoteCompilerPlugin : CompilerPluginRegistrar() {

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }

//        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
//        configuration.kotlinSourceRoots.forEach {
//            messageCollector.report(
//                CompilerMessageSeverity.WARNING,
//                "*** Hello from ***" + it.path
//            )
//        }
//        val logging = true

        registerRpcExtensions(configuration)
    }
}

@OptIn(ExperimentalCompilerApi::class)
fun CompilerPluginRegistrar.ExtensionStorage.registerRpcExtensions(configuration: CompilerConfiguration) {
    IrGenerationExtension.registerExtension(ExampleIrGenerationExtension(DebugLogger(false, MessageCollector.NONE)))
    IrGenerationExtension.registerExtension(RpcIrExtension())
}


@OptIn(ExperimentalCompilerApi::class)
class KRemoteCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "kotlinx-kremote"

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = "enabled", valueDescription = "<true|false>",
            description = "whether to enable the plugin or not"
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        "enabled" -> configuration.put(KEY_ENABLED, value.toBoolean())
        else -> configuration.put(KEY_ENABLED, true)
    }
}

private val KEY_ENABLED = CompilerConfigurationKey<Boolean>("whether the plugin is enabled")
