package kotlinx.kremote.codegen.backend

import kotlinx.kremote.codegen.common.RpcNames
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.getNameWithAssert
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

data class RemoteClass(val declaration: IrClass, val serializer: IrClass, val stub: IrClass)

class RemoteClassScanner: IrElementTransformerVoid() {
    val remoteClasses = mutableListOf<RemoteClass>()
    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.remoteSerializable()) {
            val nestedClasses = declaration.declarations.filterIsInstance<IrClass>()
            val stub = nestedClasses.singleOrNull { it.name == RpcNames.REMOTE_CLASS_STUB_NAME }
                ?: error("Generated stub not found for remote class ${declaration.getNameWithAssert()}")
            val serializer = nestedClasses.singleOrNull { it.name == RpcNames.REMOTE_CLASS_SERIALIZER_NAME }
                ?: error("Generated serializer not found for remote class ${declaration.getNameWithAssert()}")
            remoteClasses.add(RemoteClass(declaration, serializer, stub))
        }
        return super.visitClass(declaration)
    }
}