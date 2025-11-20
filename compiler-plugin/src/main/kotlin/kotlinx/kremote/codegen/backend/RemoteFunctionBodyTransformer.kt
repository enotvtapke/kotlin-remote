package kotlinx.kremote.codegen.backend

import kotlinx.kremote.codegen.common.RpcClassId.remoteAnnotation
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name

internal class RemoteFunctionBodyTransformer : IrTransformer<RpcIrContext>() {
    private fun RpcIrContext.irBuilder(symbol: IrSymbol): DeclarationIrBuilder =
        DeclarationIrBuilder(pluginContext, symbol, symbol.owner.startOffset, symbol.owner.endOffset)

    override fun visitFunction(
        declaration: IrFunction,
        data: RpcIrContext
    ): IrStatement {
        if (!declaration.remote()) return super.visitFunction(declaration, data)
        if (!declaration.isTopLevel && !declaration.isStatic) {
            error("Remote function `${declaration.name}` can't be a non-static method")
        }
        val originalBody = declaration.body ?: error("Remote function `${declaration.name}` should have a body")
        val context = declaration.parameters.singleOrNull {
            it.type == data.remoteContext.defaultType && it.kind == IrParameterKind.Context
        } ?: error("Remote function should have a context parameter of type ${data.remoteContext.defaultType.render()}")
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
            +irReturn(irCall(call).apply {
                typeArguments[0] =
                    if (isStreaming) (declaration.returnType as IrSimpleType).arguments.single().typeOrFail
                    else declaration.returnType
                arguments[0] = configClient
                arguments[1] = irCallConstructor(data.remoteCall.constructors.single(), listOf()).apply {
                    arguments[0] = irString(declaration.name.asString())
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
            })
        }
        return super.visitFunction(declaration, data)
    }

    private fun IrFunction.valueParameters(): List<IrValueParameter> =
        parameters.filter { it.kind == IrParameterKind.Regular }

    private fun IrDeclaration.remoteConfigObject(): IrClassSymbol {
        val remoteAnnotationCall = getAnnotation(remoteAnnotation.asSingleFqName())!!
        val remoteConfigClassExpression = remoteAnnotationCall.getValueArgument(Name.identifier("config"))
            ?: error(
                "Annotation '${remoteAnnotation.asSingleFqName().asString()}' should have an argument named `config`"
            )
        val remoteConfigSymbol =
            ((remoteConfigClassExpression.type as? IrSimpleType)?.arguments[0] as? IrTypeProjection)?.type?.classOrFail
                ?: error("Cannot get RemoteConfig from type ${remoteConfigClassExpression.type}")
        return remoteConfigSymbol
    }
}