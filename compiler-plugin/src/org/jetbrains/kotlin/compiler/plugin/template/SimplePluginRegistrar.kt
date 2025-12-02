package org.jetbrains.kotlin.compiler.plugin.template

import kotlinx.remote.codegen.frontend.FirRemoteAdditionalCheckers
import kotlinx.remote.codegen.frontend.FirRemoteClassTransformer
import org.jetbrains.kotlin.compiler.plugin.template.fir.SimpleClassGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SimplePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SimpleClassGenerator
        +::FirRemoteClassTransformer
        +::FirRemoteAdditionalCheckers
    }
}
