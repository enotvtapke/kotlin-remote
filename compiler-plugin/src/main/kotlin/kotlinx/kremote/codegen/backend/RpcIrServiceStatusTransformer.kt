/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.kremote.codegen.backend

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import kotlin.sequences.forEach

internal class RpcIrServiceStatusTransformer : IrTransformer<RpcIrContext>() {
    override fun visitClass(
        declaration: IrClass,
        data: RpcIrContext
    ): IrStatement {
        if (!declaration.remoteSerializable()) return declaration
        declaration.modality = Modality.OPEN
        return declaration
    }
}