package kotlinx.remote.codegen

import kotlinx.remote.codegen.frontend.FirRemoteAdditionalCheckers
import kotlinx.remote.codegen.frontend.FirRemoteClassTransformer
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SimplePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirRemoteClassTransformer
        +::FirRemoteAdditionalCheckers
    }
}
