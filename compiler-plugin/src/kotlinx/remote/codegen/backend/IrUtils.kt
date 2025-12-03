/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.backend

import org.jetbrains.kotlin.descriptors.ClassKind
import kotlinx.remote.codegen.common.RemoteClassId.remoteAnnotation
import kotlinx.remote.codegen.common.RemoteClassId.remoteSerializableAnnotation
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.ir.types.getClass

fun IrClassifierSymbol.typeWith(type: IrType, variance: Variance): IrType {
    return IrSimpleTypeImpl(
        classifier = this,
        nullability = SimpleTypeNullability.NOT_SPECIFIED,
        arguments = listOf(makeTypeProjection(type, variance)),
        annotations = emptyList(),
    )
}

val IrProperty.getterOrFail: IrSimpleFunction get () {
    return getter ?: error("'getter' should be present, but was null: ${dump()}")
}

fun IrProperty.addDefaultGetter(
    parentClass: IrClass,
    builtIns: IrBuiltIns,
    configure: IrSimpleFunction.() -> Unit = {},
) {
    addDefaultGetter(parentClass, builtIns)

    getterOrFail.apply {
        dispatchReceiverParameter!!.origin = IrDeclarationOrigin.DEFINED

        configure()
    }
}

fun IrDeclaration.remote(): Boolean = hasAnnotation(remoteAnnotation)

fun IrDeclaration.remoteSerializable(): Boolean = hasAnnotation(remoteSerializableAnnotation)

fun RemoteIrContext.irBuilder(symbol: IrSymbol): DeclarationIrBuilder =
    DeclarationIrBuilder(pluginContext, symbol, symbol.owner.startOffset, symbol.owner.endOffset)

fun IrDeclaration.remoteConfigObject(): IrClassSymbol {
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

fun IrBuilder.remoteConfigContextCall(declaration: IrDeclaration, context: RemoteIrContext): IrCall {
    val remoteConfigSymbol = declaration.remoteConfigObject()
    return irCall(context.remoteConfigContext.owner.getter!!.symbol).apply {
        arguments[0] = irGetObjectValue(remoteConfigSymbol.defaultType, remoteConfigSymbol)
    }
}

fun IrFunction.remoteFunctionName(): String = fqNameWhenAvailable?.asString()
    ?: error("Remote function `${name.asString()}` doesn't have fully qualified name")

fun IrBuilderWithScope.irSafeAs(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.SAFE_CAST, type, argument)

fun IrFunction.nonStaticParameters(ctx: RemoteIrContext): List<IrValueParameter> = parameters.filter { param ->
    if (param.isRemoteContext(ctx)) return@filter false

    val dispatch = dispatchReceiverParameter
    if (param == dispatch) {
        val receiverClass = dispatch.type.getClass()
        if (receiverClass?.kind == ClassKind.OBJECT) return@filter false
    }

    true
}

fun IrValueParameter.isRemoteContext(ctx: RemoteIrContext) = kind == IrParameterKind.Context && type.isSubtypeOfClass(ctx.remoteContext)