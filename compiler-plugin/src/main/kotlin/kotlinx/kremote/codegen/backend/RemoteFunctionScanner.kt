package kotlinx.kremote.codegen.backend

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class RemoteFunctionScanner: IrElementTransformerVoid() {
    val remoteFunctions = mutableListOf<IrFunction>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.remote()) {
            remoteFunctions.add(declaration)
        }
        return super.visitFunction(declaration)
    }
}