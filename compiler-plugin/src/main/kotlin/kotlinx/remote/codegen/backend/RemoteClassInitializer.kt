package kotlinx.remote.codegen.backend

import kotlinx.remote.codegen.common.RemoteNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.*
import kotlin.properties.Delegates

class RemoteClassInitializer(private val ctx: RemoteIrContext) {
    fun init(remoteClass: RemoteClass) {
        initStubClass(remoteClass.stub, remoteClass.declaration)
        initSerializer(remoteClass.serializer)
    }

    private var stubConstructor: IrConstructor by Delegates.notNull()

    private fun initStubClass(stubClass: IrClass, remoteClass: IrClass) {
        stubClass.declarations.removeAll { declaration ->
            declaration.isFakeOverride && declaration.getNameWithAssert() == ctx.properties.stubId.owner.name
        }
        val stubId = stubClass.addConstructor {
            name = stubClass.name
            isPrimary = true
            returnType = stubClass.defaultType
        }.run {
            stubConstructor = this
            body = ctx.irBuilder(symbol).irBlockBody {
                +irDelegatingConstructorCall(remoteClass.constructors.firstOrNull { constructor ->
                    constructor.parameters.all { it.defaultValue != null }
                } ?: error("Class ${remoteClass.name} has no default constructor"))
                +IrInstanceInitializerCallImpl(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    classSymbol = stubClass.symbol,
                    type = context.irBuiltIns.unitType,
                )
            }
            addValueParameter {
                name = ctx.properties.stubId.owner.name
                type = ctx.irBuiltIns.longType
            }
        }

        stubClass.addProperty {
            name = ctx.properties.stubId.owner.name
        }.apply {
            addBackingField {
                visibility = PRIVATE
                type = ctx.irBuiltIns.longType
                isFinal = true
                overriddenSymbols = listOf(ctx.properties.stubId)
            }.apply {
                initializer = factory.createExpressionBody(
                    IrGetValueImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = stubId.type,
                        symbol = stubId.symbol,
                        origin = IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER,
                    )
                )
            }
            addDefaultGetter(stubClass, ctx.irBuiltIns) {
                overriddenSymbols = listOf(ctx.properties.stubId.owner.getterOrFail.symbol)
            }
        }
    }

    private fun initSerializer(serializerClass: IrClass) {
        serializerClass.addConstructor {
            name = serializerClass.name
            isPrimary = true
        }.also { constructor ->
            constructor.body = ctx.irBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(ctx.remoteSerializer.owner.constructors.single())
                +IrInstanceInitializerCallImpl(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    classSymbol = serializerClass.symbol,
                    type = context.irBuiltIns.unitType,
                )
            }
        }

        serializerClass.functions.single {
            it.name == RemoteNames.REMOTE_SERIALIZER_STUB_NAME
        }.apply {
            isFakeOverride = false
            origin = IrDeclarationOrigin.DEFINED
            modality = Modality.FINAL
            val id = parameters.single { it.name == ctx.properties.stubId.owner.name }
            body = ctx.irBuilder(symbol).irBlockBody {
                +irReturn(
                    irCallConstructor(
                        stubConstructor.symbol,
                        listOf()
                    ).apply {
                        arguments[0] = irGet(id)
                    }
                )
            }
        }
    }
}