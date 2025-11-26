package kotlinx.kremote.codegen.frontend

import kotlinx.kremote.codegen.common.RpcClassId
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

object FirRpcPredicates {
    internal val remoteSerializable = DeclarationPredicate.create {
        annotated(RpcClassId.remoteSerializableAnnotation.asSingleFqName())
    }
}
