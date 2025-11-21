package kotlinx.kremote.codegen.backend

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal class RemoteFunctionBodyTransformer : IrTransformer<RpcIrContext>() {
    override fun visitFunction(
        declaration: IrFunction,
        data: RpcIrContext
    ): IrStatement {
        if (!declaration.remote()) return super.visitFunction(declaration, data)
        if (!(declaration.isTopLevel || declaration.isLocal || declaration.isStatic)) {
            error("Remote function `${declaration.name}` can't be a non-static method")
        }
        val originalBody = declaration.body ?: error("Remote function `${declaration.name}` should have a body")
        val context = declaration.parameters.singleOrNull {
            it.type == data.remoteContext.defaultType && it.kind == IrParameterKind.Context
        }
            ?: error("Remote function `${declaration.name}` should have a single context parameter of type ${data.remoteContext.defaultType.render()}")
        val remoteConfigSymbol = declaration.remoteConfigObject()
        declaration.body = data.irBuilder(declaration.symbol).irBlockBody {
            val configClient = irCall(data.remoteConfigClient.owner.getter!!.symbol).apply {
                arguments[0] = irGetObjectValue(remoteConfigSymbol.defaultType, remoteConfigSymbol)
            }
            val configContext = irCall(data.remoteConfigContext.owner.getter!!.symbol).apply {
                arguments[0] = irGetObjectValue(remoteConfigSymbol.defaultType, remoteConfigSymbol)
            }
            +irWhen(
                data.irBuiltIns.unitType,
                listOf(
                    irBranch(
                        irEquals(irGet(context), configContext),
                        when (originalBody) {
                            is IrBlockBody -> irBlock {
                                originalBody.statements.forEach { +it }
                            }

                            is IrExpressionBody -> irReturn(originalBody.expression)
                            is IrSyntheticBody -> error("Remote function can't have synthetic body")
                        }
                    )
                )
            )
            val isStreaming = declaration.returnType.isSubtypeOfClass(data.flow)
            val call =
                if (isStreaming) data.functions.remoteClientCallStreaming
                else data.functions.remoteClientCall
            val remoteCall = irCallConstructor(data.remoteCall.constructors.single(), listOf()).apply {
                arguments[0] = irString(declaration.remoteFunctionName())
                arguments[1] = if (declaration.parameters.isEmpty()) {
                    irCall(data.functions.emptyArray)
                } else {
                    irCall(data.irBuiltIns.arrayOf).apply {
                        typeArguments[0] = data.anyNullable
                        arguments[0] = irVararg(
                            elementType = data.anyNullable,
                            values = declaration.valueParameters().memoryOptimizedMap {
                                irGet(it)
                            },
                        )
                    }
                }
            }
            +irReturn(irCall(call).apply {
                typeArguments[0] =
                    if (isStreaming) (declaration.returnType as IrSimpleType).arguments.single().typeOrFail
                    else declaration.returnType
                arguments[0] = configClient
                arguments[1] = remoteCall
            })
        }
        return super.visitFunction(declaration, data)
    }
}