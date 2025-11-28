/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.backend

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal class RemoteClassStatusTransformer : IrTransformer<RemoteIrContext>() {
    override fun visitClass(
        declaration: IrClass,
        data: RemoteIrContext
    ): IrStatement {
        if (!declaration.remoteSerializable()) return declaration
        declaration.modality = Modality.OPEN
        return declaration
    }
}