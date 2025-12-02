package kotlinx.remote.codegen.frontend.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement
import kotlin.getValue

object FirRemoteDiagnostics : KtDiagnosticsContainer() {
    val WRONG_REMOTE_FUNCTION_CONTEXT by error1<KtElement, ConeKotlinType>()
    val GENERIC_REMOTE_FUNCTION by error0<KtElement>()
    val NON_SUSPENDING_REMOTE_FUNCTION by error0<KtElement>()
    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    private object Renderers : BaseDiagnosticRendererFactory() {

        override val MAP: KtDiagnosticFactoryToRendererMap by rendererMap
    }
}

private val rendererMap: Lazy<KtDiagnosticFactoryToRendererMap> = KtDiagnosticFactoryToRendererMap("Remote") { map ->
    map.put(
        factory = FirRemoteDiagnostics.WRONG_REMOTE_FUNCTION_CONTEXT,
        message = "Remote function should have exactly one context parameter of type {0}.",
        rendererA = FirDiagnosticRenderers.RENDER_TYPE,
    )

    map.put(
        factory = FirRemoteDiagnostics.GENERIC_REMOTE_FUNCTION,
        message = "Remote function cannot have type parameters.",
    )

    map.put(
        factory = FirRemoteDiagnostics.NON_SUSPENDING_REMOTE_FUNCTION,
        message = "Remote function should be suspend.",
    )
}