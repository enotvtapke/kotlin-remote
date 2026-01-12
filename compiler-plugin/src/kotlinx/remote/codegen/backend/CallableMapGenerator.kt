package kotlinx.remote.codegen.backend

import kotlinx.remote.codegen.common.RemoteClassId
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class CallableMapGenerator(private val ctx: RemoteIrContext, private val remoteFunctions: MutableList<IrFunction>) {

    fun generate(parent: IrDeclarationParent): IrExpression {
        return IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.callableMap.defaultType,
            symbol = ctx.callableMap.constructors.single(),
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        ).apply {
            arguments[0] = irMapOf(
                ctx.irBuiltIns.stringType,
                ctx.remoteCallable.defaultType,
                remoteFunctions.map {
                    IrConstImpl.string(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        ctx.irBuiltIns.stringType,
                        it.remoteFunctionName()
                    ) to irRpcCallable(parent, it)
                }
            )
        }
    }

    private fun irRpcCallable(parent: IrDeclarationParent, callable: IrFunction): IrExpression {
        return IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.remoteCallable.defaultType,
            symbol = ctx.remoteCallable.constructors.single(),
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        ).apply {
            val invokator = genInvocator(parent, callable)
            val parameters = callable.nonStaticParameters(ctx)
            val callee =
                if (parameters.isEmpty()) ctx.functions.emptyArray
                else ctx.irBuiltIns.arrayOf
            val parametersType = ctx.irBuiltIns.arrayClass.typeWith(
                ctx.remoteParameter.defaultType,
                Variance.OUT_VARIANCE,
            )
            val parametersCall =
                IrCallImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = parametersType,
                    symbol = callee,
                    typeArgumentsCount = 1,
                ).apply arrayOfCall@{
                    if (callee == ctx.functions.emptyArray) {
                        typeArguments[0] = ctx.remoteParameter.defaultType
                        return@arrayOfCall
                    }
                    val vararg = IrVarargImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = parametersType,
                        varargElementType = ctx.remoteParameter.defaultType,
                        elements = parameters.memoryOptimizedMap { parameter ->
                            IrConstructorCallImpl(
                                startOffset = UNDEFINED_OFFSET,
                                endOffset = UNDEFINED_OFFSET,
                                type = ctx.remoteParameter.defaultType,
                                symbol = ctx.remoteParameter.constructors.single(),
                                typeArgumentsCount = 0,
                                constructorTypeArgumentsCount = 0,
                            ).apply {
                                arguments[0] = IrConstImpl.string(
                                    startOffset,
                                    endOffset,
                                    ctx.irBuiltIns.stringType,
                                    parameter.name.asString()
                                )
                                arguments[1] = irRemoteTypeCall(
                                    parameter.type,
                                    isPolymorphic = parameter.type.hasPolymorphicAnnotation()
                                )
                                arguments[2] = IrConstImpl.boolean(
                                    startOffset,
                                    endOffset,
                                    ctx.irBuiltIns.booleanType,
                                    parameter.defaultValue != null
                                )
                            }
                        },
                    )
                    typeArguments[0] = ctx.remoteParameter.defaultType
                    arguments[0] = vararg
                }
            arguments[0] =
                IrConstImpl.string(startOffset, endOffset, ctx.irBuiltIns.stringType, callable.name.asString())
            arguments[1] = irRemoteTypeCall(
                ctx.remoteResponse.typeWith(listOf(callable.returnType)),
                isPolymorphic = callable.returnType.type.hasPolymorphicAnnotation()
            )
            arguments[2] = invokator
            arguments[3] = parametersCall
        }
    }

    private fun genInvocator(parent: IrDeclarationParent, callable: IrFunction): IrExpression {
        val functionLambda = callable.factory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
            returnType = ctx.anyNullable
            isSuspend = true
        }.apply {
            this.parent = parent

            val contextParameter = IrValueParameterBuilder().run {
                kind = IrParameterKind.Context
                type = ctx.remoteContext.typeWith(ctx.nothing.defaultType)
                name = Name.identifier("ctx")
                factory.buildValueParameter(this, this@apply).also { valueParameter ->
                    parameters += valueParameter
                }
            }

            val parametersParameter = addValueParameter {
                name = Name.identifier("parameters")
                type = ctx.arrayOfAnyNullable
            }

            body = ctx.irBuilder(symbol).irBlockBody {
                val call = irCall(callable).apply {
                    val dispatch = callable.dispatchReceiverParameter
                    if (dispatch != null) {
                        val receiverClass = dispatch.type.getClass()
                        if (receiverClass != null && receiverClass.kind == ClassKind.OBJECT) {
                            arguments[dispatch.indexInParameters] = irGetObjectValue(receiverClass.symbol.defaultType, receiverClass.symbol)
                        }
                    }
                    callable.parameters.filter { it.isRemoteContext(ctx) }.forEach {
                        arguments[it.indexInParameters] = irGet(contextParameter)
                    }
                    callable.nonStaticParameters(ctx).forEachIndexed { index, param ->
                        val argValue = irCall(
                            callee = ctx.functions.arrayGet.symbol,
                            type = ctx.anyNullable,
                            origin = IrStatementOrigin.GET_ARRAY_ELEMENT,
                        ).apply {
                            arguments[0] = irGet(parametersParameter)
                            arguments[1] = IrConstImpl.int(
                                startOffset = startOffset,
                                endOffset = endOffset,
                                type = ctx.irBuiltIns.intType,
                                value = index,
                            )
                        }
                        arguments[param.indexInParameters] = if (param.type.isNullable()) irSafeAs(argValue, param.type) else irAs(argValue, param.type)
                    }
                }
                +irReturn(call)
            }
        }
        return IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.remoteInvokator.defaultType,
            operator = IrTypeOperator.SAM_CONVERSION,
            typeOperand = ctx.remoteInvokator.defaultType,
            argument = IrFunctionExpressionImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = ctx.suspendFunction1.typeWith(
                    ctx.anyNullable,
                    ctx.anyNullable,
                ),
                origin = IrStatementOrigin.LAMBDA,
                function = functionLambda,
            ),
        )
    }

    private fun irRemoteTypeCall(type: IrType, isPolymorphic: Boolean = false): IrConstructorCallImpl {
        return IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.remoteType.defaultType,
            symbol = ctx.remoteType.constructors.single(),
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        ).apply {
            arguments[0] = irTypeOfCall(substituteTypeParametersWithUpperBounds(type))
            arguments[1] = IrConstImpl.boolean(
                startOffset,
                endOffset,
                ctx.irBuiltIns.booleanType,
                isPolymorphic
            )
        }
    }

    private fun IrType.hasPolymorphicAnnotation(): Boolean {
        return hasAnnotation(RemoteClassId.polymorphicAnnotation.asSingleFqName())
    }

    private fun irTypeOfCall(type: IrType): IrCall {
        return IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.kTypeClass.defaultType,
            symbol = ctx.functions.typeOf,
            typeArgumentsCount = 1,
        ).apply {
            typeArguments[0] = type
        }
    }

    private fun substituteTypeParametersWithUpperBounds(type: IrType): IrType {
        return when (type) {
            is IrSimpleType -> {
                val classifier = type.classifier
                if (classifier is IrTypeParameterSymbol) {
                    val upperBound = classifier.owner.superTypes.firstOrNull() ?: ctx.anyNullable
                    val substituted = substituteTypeParametersWithUpperBounds(upperBound)
                    if (type.isNullable()) substituted.makeNullable() else substituted.makeNotNull()
                } else {
                    val newArgs: List<IrTypeArgument> = type.arguments.memoryOptimizedMap { arg ->
                        when (arg) {
                            is IrTypeProjection -> makeTypeProjection(
                                substituteTypeParametersWithUpperBounds(arg.type),
                                arg.variance
                            )
                            else -> arg
                        }
                    }
                    IrSimpleTypeImpl(
                        classifier = classifier,
                        nullability = type.nullability,
                        arguments = newArgs,
                        annotations = type.annotations,
                    )
                }
            }
            else -> type
        }
    }

    private fun irMapOf(
        keyType: IrType,
        valueType: IrType,
        elements: List<Pair<IrExpression, IrExpression>>,
    ): IrCallImpl {
        return IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.irBuiltIns.mapClass.typeWith(keyType, valueType),
            symbol = if (elements.isEmpty()) ctx.functions.emptyMap else ctx.functions.mapOf,
            origin = IrStatementOrigin.INITIALIZE_FIELD,
            typeArgumentsCount = 2,
        ).apply mapApply@{
            if (elements.isEmpty()) {
                typeArguments[0] = keyType
                typeArguments[1] = valueType

                return@mapApply
            }

            val pairType = ctx.pair.typeWith(keyType, valueType)

            val varargType = ctx.irBuiltIns.arrayClass.typeWith(pairType, Variance.OUT_VARIANCE)

            val vararg = IrVarargImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = varargType,
                varargElementType = pairType,
                elements = elements.memoryOptimizedMap { (key, value) ->
                    IrCallImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = pairType,
                        symbol = ctx.functions.to,
                        typeArgumentsCount = 2,
                    ).apply {
                        typeArguments[0] = keyType
                        typeArguments[1] = valueType
                        arguments[0] = key
                        arguments[1] = value

                    }
                },
            )
            typeArguments[0] = keyType
            typeArguments[1] = valueType
            arguments[0] = vararg
        }
    }
}