package kotlinx.remote.codegen.backend

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class RemoteFunctionBodyTransformer : IrTransformer<RemoteIrContext>() {
    override fun visitFunction(
        declaration: IrFunction,
        data: RemoteIrContext
    ): IrStatement {
        if (!declaration.remote() || declaration.isFakeOverride) return super.visitFunction(declaration, data)
        val originalBody = declaration.body ?: error("Remote function `${declaration.name}` should have a body")
        val context = declaration.parameters.singleOrNull {
            it.isRemoteWrapper(data)
        } ?: error("Remote function `${declaration.name}` should have a single context parameter of type RemoteWrapper<${data.remoteContext.defaultType.render()}>")
        declaration.body = data.irBuilder(declaration.symbol).irBlockBody {
            val wrappedRemoteContext = irAs(irGet(context), data.wrappedRemote.defaultType)
            val innerContext = irCall(data.wrappedRemoteContext.owner.getter!!.symbol).apply {
                arguments[0] = wrappedRemoteContext
            }
            val contextClient = irCall(data.remoteContextClient.owner.getter!!.symbol).apply {
                arguments[0] = innerContext
            }
            val remoteCall = irCallConstructor(data.remoteCall.constructors.single(), listOf()).apply {
                arguments[0] = irString(declaration.remoteFunctionName())
                arguments[1] = if (declaration.parameters.isEmpty()) {
                    irCall(data.functions.emptyArray)
                } else {
                    irCall(data.irBuiltIns.arrayOf).apply {
                        typeArguments[0] = data.anyNullable
                        arguments[0] = irVararg(
                            elementType = data.anyNullable,
                            values = declaration.nonStaticParameters(data).memoryOptimizedMap {
                                irGet(it)
                            },
                        )
                    }
                }
            }
            +irWhen(
                data.irBuiltIns.unitType,
                listOf(
                    irBranch(
                        irIs(irGet(context), data.local.defaultType),
                        irBlock {
                            declaration.dispatchReceiverParameter?.also {
                                +irCall(data.functions.checkIsNotStubForRemoteClassMethod).apply { arguments[0] = irGet(it) }
                            }
                            when (originalBody) {
                                is IrBlockBody -> originalBody.statements.forEach { +it }
                                is IrExpressionBody -> +irReturn(originalBody.expression)
                                is IrSyntheticBody -> error("Remote function can't have synthetic body")
                            }
                        }
                    ),
                    irBranch(
                        true.toIrConst(data.irBuiltIns.booleanType, startOffset, endOffset),
                        irReturn(irCall(data.functions.remoteClientCall).apply {
                            typeArguments[0] = declaration.returnType
                            arguments[0] = contextClient
                            arguments[1] = remoteCall
                        })
                    )
                )
            )
        }
        return super.visitFunction(declaration, data)
    }
}