package kotlinx.kremote.codegen.frontend

import kotlinx.kremote.codegen.common.RpcClassId
import kotlinx.kremote.codegen.common.RpcNames
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.Name

class FirRpcServiceGenerator(
    session: FirSession,
    @Suppress("unused")
    private val logger: MessageCollector,
) : FirDeclarationGenerationExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirRpcPredicates.remoteSerializable)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
        return if (session.predicateBasedProvider.matches(FirRpcPredicates.remoteSerializable, classSymbol)) {
            setOf(RpcNames.REMOTE_CLASS_STUB_NAME, RpcNames.REMOTE_CLASS_SERIALIZER_NAME)
        } else {
            emptySet()
        }
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return when(name) {
            RpcNames.REMOTE_CLASS_STUB_NAME -> {
                generateRpcServiceStubClass(owner)
            }

            RpcNames.REMOTE_CLASS_SERIALIZER_NAME -> {
                generateSerializerObjectForRpcService(owner)
            }

            else -> {
                error("Cannot run generation for ${owner.classId.createNestedClassId(name).asSingleFqName()}")
            }
        }
    }

    private fun generateSerializerObjectForRpcService(owner: FirClassSymbol<*>, ): FirClassLikeSymbol<*> {
        return createNestedClass(owner, RpcNames.REMOTE_CLASS_SERIALIZER_NAME, FirRemoteClassSerializerKey, ClassKind.OBJECT) {
            visibility = Visibilities.Public
            modality = Modality.FINAL
            val typeArguments = arrayOf(owner.classId.constructClassLikeType())
            superType(RpcClassId.kSerializer.constructClassLikeType(typeArguments))
        }.symbol
    }

    private fun generateRpcServiceStubClass(owner: FirClassSymbol<*>): FirRegularClassSymbol? {
        return createNestedClass(owner, RpcNames.REMOTE_CLASS_STUB_NAME, FirRemoteClassStubKey) {
            visibility = Visibilities.Public
            modality = Modality.FINAL
            superType(owner.classId.constructClassLikeType())
            superType(RpcClassId.stubInterface.constructClassLikeType())
        }.symbol
    }
}
