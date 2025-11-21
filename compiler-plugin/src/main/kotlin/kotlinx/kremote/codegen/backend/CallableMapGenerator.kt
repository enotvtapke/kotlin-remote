package kotlinx.kremote.codegen.backend

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

class CallableMapGenerator(private val ctx: RpcIrContext, private val remoteFunctions: MutableList<IrFunction>) {

    fun generate(parent: IrFunction): IrExpression {
        return irMapOf(
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

    private fun irRpcCallable(parent: IrFunction, callable: IrFunction): IrExpression {
        return IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.remoteCallable.defaultType,
            symbol = ctx.remoteCallable.constructors.single(),
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        ).apply {
            val invokator = genInvocator(parent, callable)
            val parameters = callable.valueParameters()
            val callee =
                if (callable.valueParameters().isEmpty()) ctx.functions.emptyArray
                else ctx.irBuiltIns.arrayOf
            val arrayParametersType = ctx.irBuiltIns.arrayClass.typeWith(
                ctx.remoteParameter.defaultType,
                Variance.OUT_VARIANCE,
            )
            val arrayOfCall =
                IrCallImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = arrayParametersType,
                    symbol = callee,
                    typeArgumentsCount = 1,
                ).apply arrayOfCall@{
                    if (parameters.isEmpty()) {
                        typeArguments[0] = ctx.remoteParameter.defaultType
                        return@arrayOfCall
                    }
                    val vararg = IrVarargImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = arrayParametersType,
                        varargElementType = ctx.remoteParameter.defaultType,
                        elements = parameters.memoryOptimizedMap { parameter ->
                            IrConstructorCallImpl(
                                startOffset = UNDEFINED_OFFSET,
                                endOffset = UNDEFINED_OFFSET,
                                type = ctx.remoteParameter.defaultType,
                                symbol = ctx.remoteParameter.constructors.single(),
                                typeArgumentsCount = 0,
                                constructorTypeArgumentsCount = 0,
                            )
                                .apply {
                                    arguments[0] = IrConstImpl.string(
                                        startOffset,
                                        endOffset,
                                        ctx.irBuiltIns.stringType,
                                        parameter.name.asString()
                                    )
                                    arguments[1] = irRemoteTypeCall(parameter.type)
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
            val isStreaming = callable.returnType.isSubtypeOfClass(ctx.flow)
            arguments[0] =
                IrConstImpl.string(startOffset, endOffset, ctx.irBuiltIns.stringType, callable.name.asString())
            arguments[1] = irRemoteTypeCall(
                if (isStreaming) (callable.returnType as IrSimpleType).arguments.single().typeOrFail
                else callable.returnType
            )
            arguments[2] = invokator
            arguments[3] = arrayOfCall
            arguments[4] =
                IrConstImpl.boolean(startOffset, endOffset, ctx.irBuiltIns.booleanType, isStreaming)

        }
    }

    private fun genInvocator(parent: IrFunction, callable: IrFunction): IrExpression {
        val functionLambda = parent.factory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
            returnType = ctx.anyNullable
            isSuspend = true
        }.apply {
            this.parent = parent

            val parametersParameter = addValueParameter {
                name = Name.identifier("parameters")
                type = ctx.arrayOfAnyNullable
            }

            body = ctx.irBuilder(symbol).irBlockBody {
                val call = irCall(callable).apply {
                    val argValues = callable.valueParameters().memoryOptimizedMapIndexed { argIndex, arg ->
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
                                value = argIndex,
                            )
                        }
                        if (arg.type.isNullable()) irSafeAs(argValue, arg.type) else irAs(argValue, arg.type)
                    }
                    val remoteContextParameter = callable.parameters.singleOrNull {
                        it.kind == IrParameterKind.Context && it.type.isSubtypeOfClass(ctx.remoteContext)
                    }
                        ?: error("Remote function `${callable.name}` should have a single context parameter of type `${ctx.remoteContext.defaultType.render()}`")
                    arguments[remoteContextParameter.indexInParameters] = remoteConfigContextCall(callable, ctx)
                    callable.parameters.filter { it.kind == IrParameterKind.Regular }.forEachIndexed { index, param ->
                        arguments[param.indexInParameters] = argValues[index]
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

    private fun irRemoteTypeCall(type: IrType): IrConstructorCallImpl {
        return IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.remoteType.defaultType,
            symbol = ctx.remoteType.constructors.single(),
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        ).apply {
            arguments[0] = irTypeOfCall(type)
        }
    }

    private fun irTypeOfCall(type: IrType): IrCall {
        return IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.kTypeClass.defaultType,
            symbol = ctx.functions.typeOf,
            typeArgumentsCount = 1,
        )
            .apply {
                typeArguments[0] = type
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