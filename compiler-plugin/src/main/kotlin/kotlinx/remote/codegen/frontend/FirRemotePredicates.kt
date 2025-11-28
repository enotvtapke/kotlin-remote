package kotlinx.remote.codegen.frontend

import kotlinx.remote.codegen.common.RemoteClassId
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

object FirRemotePredicates {
    internal val remoteSerializable = DeclarationPredicate.create {
        annotated(RemoteClassId.remoteSerializableAnnotation.asSingleFqName())
    }
}
