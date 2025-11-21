package kotlinx.kremote.codegen.backend

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class CallableMapInjector(private val ctx: RpcIrContext, private val generator: CallableMapGenerator): IrElementTransformerVoid() {
    var parent: IrFunction? = null

    override fun visitFunction(declaration: IrFunction): IrStatement {
        parent = declaration
        return super.visitFunction(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol == ctx.functions.callableMapInit.symbol) {
            expression.arguments[1] = generator.generate(parent!!)
        }
        return super.visitCall(expression)
    }
}