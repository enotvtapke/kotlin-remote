package kotlinx.remote.codegen.frontend

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension.Factory as GFactory

class FirRemoteExtensionRegistrar(private val configuration: CompilerConfiguration) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        val logger = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        +GFactory { FirRemoteClassTransformer(it, logger) }
    }
}
