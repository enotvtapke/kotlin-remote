package kotlinx.kremote.codegen.backend

import kotlinx.kremote.codegen.common.RpcNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import kotlin.properties.Delegates

class RemoteSerializerGenerator(private val ctx: RpcIrContext) {
    fun generate(remoteClass: RemoteClass) {
        initStubClass(remoteClass.declaration, remoteClass.stub)
        generateSerializer(remoteClass.serializer, remoteClass.stub)
    }

    private fun initStubClass(parent: IrClass, stub: IrClass) {
        stub.declarations.removeAll { declaration ->
            declaration.isFakeOverride && declaration.getNameWithAssert() in listOf(ctx.properties.stubId.owner.name)
        }
        stub.addConstructor {
            name = stub.name
            isPrimary = true
            returnType = stub.defaultType
        }.apply {
            val stubId = addValueParameter {
                name = ctx.properties.stubId.owner.name
                type = ctx.irBuiltIns.longType
            }
            stub.addProperty {
                name = ctx.properties.stubId.owner.name
            }.apply {
                addBackingFieldUtil {
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

                addDefaultGetter(stub, ctx.irBuiltIns) {
                    overriddenSymbols = listOf(ctx.properties.stubId.owner.getterOrFail.symbol)
                }
            }
            body = ctx.irBuilder(symbol).irBlockBody {
                +irDelegatingConstructorCall(parent.constructors.firstOrNull { constructor ->
                    constructor.parameters.all { it.defaultValue != null }
                } ?: error("Class ${parent.name} has no default constructor"))
                +IrInstanceInitializerCallImpl(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    classSymbol = stub.symbol,
                    type = context.irBuiltIns.unitType,
                )
            }
        }

    }

    private fun generateSerializer(serializerClass: IrClass, stubClass: IrClass) {
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
            it.name == RpcNames.REMOTE_SERIALIZER_STUB_NAME
        }.apply {
            isFakeOverride = false
            origin = IrDeclarationOrigin.DEFINED
            modality = Modality.FINAL
            val id = parameters.single { it.name == ctx.properties.stubId.owner.name }
            body = ctx.irBuilder(symbol).irBlockBody {
                +irReturn(
                    irCallConstructor(
                        stubClass.constructors.single().symbol,
                        listOf()
                    ).apply {
                        arguments[0] = irGet(id)
                    }
                )
            }
        }
    }
}