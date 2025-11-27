package kotlinx.kremote.codegen.backend

import kotlinx.kremote.codegen.common.RpcNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
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
        generateSerializer(remoteClass.declaration, remoteClass.serializer, remoteClass.stub)
    }

    private var stubId: IrProperty by Delegates.notNull()

    private fun initStubClass(parent: IrClass, stub: IrClass) {
        stub.declarations.removeAll { declaration ->
            declaration.isFakeOverride && declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE && declaration.getNameWithAssert() in listOf(ctx.properties.stubId.owner.name)
        }
        stub.addConstructor {
            name = stub.name
            isPrimary = true
            returnType = stub.defaultType
        }.apply {
            val stubId = addValueParameter {
                name = Name.identifier("id")
                type = ctx.irBuiltIns.longType
            }
            this@RemoteSerializerGenerator.stubId =
                stub.addProperty {
                    name = Name.identifier("id")
                    visibility = PRIVATE
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
                        visibility = PRIVATE
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

    private fun generateSerializer(parent: IrClass, serializerClass: IrClass, stubClass: IrClass) {
        serializerClass.superTypes = listOf(ctx.kSerializer.typeWith(parent.defaultType))
        val kSerializer = ctx.kSerializer.owner
        serializerClass.declarations.removeAll { declaration ->
            declaration.isFakeOverride &&
                    declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
                    declaration.getNameWithAssert() in listOf(
                RpcNames.KSERIALIZER_SERIALIZE_NAME,
                RpcNames.KSERIALIZER_DESERIALIZE_NAME,
                RpcNames.KSERIALIZER_DESCRIPTOR_NAME
            )
        }
        serializerClass.addConstructor {
            name = serializerClass.name
            isPrimary = true
            visibility = DescriptorVisibilities.PRIVATE
        }.also { constructor ->
            constructor.body = ctx.irBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +IrInstanceInitializerCallImpl(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    classSymbol = serializerClass.symbol,
                    type = context.irBuiltIns.unitType,
                )
            }
        }

        serializerClass.addFunction {
            name = RpcNames.KSERIALIZER_SERIALIZE_NAME
            returnType = ctx.irBuiltIns.unitType
        }.apply {
            val overriddenFunction = kSerializer.functions.single { it.name == RpcNames.KSERIALIZER_SERIALIZE_NAME }
            overriddenSymbols = listOf(overriddenFunction.symbol)
            parameters += buildReceiverParameter {
                type = serializerClass.defaultType
            }
            val (encoderParam, valueParam) = overriddenFunction.valueParameters().map {
                addValueParameter {
                    type = it.type
                    name = it.name
                }
            }
            body = ctx.irBuilder(symbol).irBlockBody {
                +irWhen(ctx.irBuiltIns.unitType, buildList {
                    add(
                        irBranch(
                            irIs(irGet(valueParam), stubClass.defaultType),
                            irReturn(
                                irCall(
                                    callee = ctx.functions.encoderEncodeLong.symbol,
                                    type = ctx.functions.encoderEncodeLong.returnType,
                                ).apply {
                                    arguments[0] = irGet(encoderParam)
                                    arguments[1] = irCallProperty(irGet(valueParam), stubId)
                                })
                        )
                    )
                })
                +irReturn(
                    irCall(
                        callee = ctx.functions.encoderEncodeLong.symbol,
                        type = ctx.functions.encoderEncodeLong.returnType,
                    ).apply {
                        arguments[0] = irGet(encoderParam)
                        arguments[1] = irCall(ctx.functions.addInstance).apply {
                            arguments[0] = irGet(valueParam)
                        }
                    })
            }
        }

        serializerClass.addFunction {
            name = RpcNames.KSERIALIZER_DESERIALIZE_NAME
            returnType = parent.defaultType
        }.apply {
            val f = this
            val overriddenFunction =
                kSerializer.functions.single { it.name == RpcNames.KSERIALIZER_DESERIALIZE_NAME }
            overriddenSymbols += overriddenFunction.symbol
            parameters += buildReceiverParameter {
                type = serializerClass.defaultType
            }
            val (decoderParam) = overriddenFunction.valueParameters().map {
                addValueParameter {
                    type = it.type
                    name = it.name
                }
            }
            body = ctx.irBuilder(symbol).irBlockBody {
                val id = IrVariableImpl(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = IrDeclarationOrigin.DEFINED,
                    symbol = IrVariableSymbolImpl(),
                    name = Name.identifier("id"),
                    type = ctx.irBuiltIns.longType,
                    isVar = false,
                    isConst = false,
                    isLateinit = false
                ).apply {
                    this.parent = f
                    initializer = irCall(ctx.functions.decoderDecodeLong.symbol).apply {
                        arguments[0] = irGet(decoderParam)
                    }
                }
                +id
                +irReturn(irAs(irCall(ctx.functions.getOrDefault).apply {
                    arguments[0] = irGetObjectValue(ctx.remoteInstancesPool.defaultType, ctx.remoteInstancesPool)
                    arguments[1] = irGet(id)
                    arguments[2] = irCallConstructor(
                        stubClass.constructors.single().symbol,
                        listOf()
                    ).apply {
                        arguments[0] = irGet(id)
                    }
                }, parent.defaultType))
            }
        }

        serializerClass.addProperty {
            name = RpcNames.KSERIALIZER_DESCRIPTOR_NAME
        }.apply {
            addBackingFieldUtil {
                type = ctx.serialDescriptor.defaultType
            }.apply {
                initializer = factory.createExpressionBody(
                    IrCallImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = ctx.serialDescriptor.defaultType,
                        symbol = ctx.functions.primitiveSerialDescriptor,
                        typeArgumentsCount = 0,
                    ).apply {
                        arguments[0] = IrConstImpl.string(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            ctx.irBuiltIns.stringType,
                            "remoteClassStubAsLongSerializer"
                        )
                        arguments[1] = IrGetObjectValueImpl(
                            startOffset = startOffset,
                            endOffset = endOffset,
                            type = ctx.primitiveKindLong.defaultType,
                            symbol = ctx.primitiveKindLong
                        )
                    }
                )
            }

            addDefaultGetter(serializerClass, ctx.irBuiltIns) {
                visibility = DescriptorVisibilities.PUBLIC
                overriddenSymbols = listOf(ctx.properties.kSerializerDescriptor.owner.getterOrFail.symbol)
            }
        }
    }

    private fun IrClass.addConstructorProperty(
        propertyName: Name,
        propertyType: IrType,
        valueParameter: IrValueParameter,
        propertyVisibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE,
    ): IrProperty {
        return addProperty {
            name = propertyName
            visibility = propertyVisibility
        }.apply {
            addBackingFieldUtil {
                visibility = DescriptorVisibilities.PRIVATE
                type = propertyType
                isFinal = true
            }.apply {
                initializer = factory.createExpressionBody(
                    IrGetValueImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = valueParameter.type,
                        symbol = valueParameter.symbol,
                        origin = IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER,
                    )
                )
            }

            addDefaultGetter(this@addConstructorProperty, ctx.irBuiltIns) {
                visibility = propertyVisibility
            }
        }
    }

    private fun irCallProperty(receiver: IrExpression, property: IrProperty): IrCall {
        val getter = property.getterOrFail

        return IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = getter.returnType,
            symbol = getter.symbol,
            origin = IrStatementOrigin.GET_PROPERTY,
            typeArgumentsCount = getter.typeParameters.size,
        ).apply {
            arguments[0] = receiver
        }
    }
}