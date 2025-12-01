/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.frontend.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers

object RemoteDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
    override val MAP by RemoteKtDiagnosticFactoryToRendererMap("Remote") { map ->
        map.put(
            factory = FirRemoteDiagnostics.WRONG_REMOTE_FUNCTION_CONTEXT,
            message = "Remote function should have exactly one context parameter of type {0}.",
            rendererA = FirDiagnosticRenderers.RENDER_TYPE,
        )

        map.put(
            factory = FirRemoteDiagnostics.GENERIC_REMOTE_FUNCTION,
            message = "Remote function cannot have type parameters.",
        )
    }
}

private fun RemoteKtDiagnosticFactoryToRendererMap(
    name: String,
    init: (KtDiagnosticFactoryToRendererMap) -> Unit,
): Lazy<KtDiagnosticFactoryToRendererMap> {
    return lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        KtDiagnosticFactoryToRendererMap(name).also(init)
    }
}
