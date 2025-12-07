package kotlinx.remote.codegen.backend

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class RemoteClassListInjector(
    private val ctx: RemoteIrContext,
    private val generator: RemoteClassListGenerator
) : IrElementTransformerVoid() {
    private var parent: IrDeclarationParent? = null

    override fun visitFunction(declaration: IrFunction): IrStatement {
        parent = declaration
        return super.visitFunction(declaration)
    }

    override fun visitExpression(expression: IrExpression): IrExpression {
        if (expression is IrCall && expression.symbol == ctx.functions.genRemoteClassList) {
            return generator.generate(parent ?: error("Cannot find call parent to inject RemoteClassList"))
        }
        return super.visitExpression(expression)
    }
}

