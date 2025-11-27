package kotlinx.kremote.codegen

import kotlinx.kremote.codegen.backend.RpcIrExtension
import kotlinx.kremote.codegen.backend.noarg.NoArgIrGenerationExtension
import kotlinx.kremote.codegen.common.RpcClassId
import kotlinx.kremote.codegen.frontend.FirRpcExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.*
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

        registerRpcExtensions(configuration)
    }
}

@OptIn(ExperimentalCompilerApi::class)
fun CompilerPluginRegistrar.ExtensionStorage.registerRpcExtensions(configuration: CompilerConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(FirRpcExtensionRegistrar(configuration))
    IrGenerationExtension.registerExtension(
        NoArgIrGenerationExtension(listOf(RpcClassId.remoteSerializableAnnotation.asSingleFqName().asString()), false)
    )
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
