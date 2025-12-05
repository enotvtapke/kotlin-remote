package kotlinx.remote.codegen.frontend

import kotlinx.remote.codegen.common.RemoteClassId
import kotlinx.remote.codegen.common.RemoteNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class FirRemoteClassTransformer(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirRemotePredicates.remoteSerializable)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
        return if (session.predicateBasedProvider.matches(FirRemotePredicates.remoteSerializable, classSymbol)) {
            setOf(RemoteNames.REMOTE_CLASS_STUB_NAME, RemoteNames.REMOTE_CLASS_SERIALIZER_NAME)
        } else {
            emptySet()
        }
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> {
        return if (classSymbol.isGeneratedByKey(FirRemoteClassStubKey)) {
            setOf(SpecialNames.INIT)
        } else {
            emptySet()
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classSymbol = context.owner
        if (!classSymbol.isGeneratedByKey(FirRemoteClassStubKey)) {
            return emptyList()
        }

        return listOf(
            createConstructor(classSymbol, FirRemoteClassStubKey, isPrimary = true) {
                visibility = Visibilities.Public
                valueParameter(Name.identifier("id"), session.builtinTypes.longType.coneType)
            }.symbol
        )
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return when(name) {
            RemoteNames.REMOTE_CLASS_STUB_NAME -> {
                generateRpcServiceStubClass(owner)
            }

            RemoteNames.REMOTE_CLASS_SERIALIZER_NAME -> {
                generateSerializerObjectForRpcService(owner)
            }

            else -> {
                error("Cannot run generation for ${owner.classId.createNestedClassId(name).asSingleFqName()}")
            }
        }
    }

    private fun generateSerializerObjectForRpcService(owner: FirClassSymbol<*>): FirClassLikeSymbol<*> {
        return createNestedClass(owner, RemoteNames.REMOTE_CLASS_SERIALIZER_NAME, FirRemoteClassSerializerKey) {
            visibility = Visibilities.Public
            modality = Modality.FINAL
            val typeArguments = arrayOf(owner.defaultType())
            superType(RemoteClassId.remoteSerializer.constructClassLikeType(typeArguments))
        }.symbol
    }

    private fun generateRpcServiceStubClass(owner: FirClassSymbol<*>): FirRegularClassSymbol? {
        return createNestedClass(owner, RemoteNames.REMOTE_CLASS_STUB_NAME, FirRemoteClassStubKey) {
            visibility = Visibilities.Public
            modality = Modality.FINAL
            superType(owner.defaultType())
            superType(RemoteClassId.stubInterface.constructClassLikeType())
        }.symbol
    }

    private fun FirClassSymbol<*>.isGeneratedByKey(key: GeneratedDeclarationKey): Boolean {
        return (origin as? FirDeclarationOrigin.Plugin)?.key == key
    }
}
