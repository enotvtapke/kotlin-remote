/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.kremote.codegen.backend

import kotlinx.kremote.codegen.common.RpcClassId.remoteAnnotation
import kotlinx.kremote.codegen.common.RpcClassId.remoteSerializableAnnotation
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.IrFieldBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
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

// A collection of functions that proved to be useful,
// but appeared only in the latest Kotlin versions.
// Copied and placed here as is

val IrTypeArgument.typeOrFail: IrType
    get() {
        require(this is IrTypeProjection) {
            "Type argument should be of type `IrTypeProjection`, but was `${this::class}` instead"
        }

        return this.type
    }

// originally named 'addBackingField'
inline fun IrProperty.addBackingFieldUtil(builder: IrFieldBuilder.() -> Unit = {}): IrField {
    return factory.buildField {
        name = this@addBackingFieldUtil.name
        origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
        builder()
    }.also { field ->
        this@addBackingFieldUtil.backingField = field
        field.correspondingPropertySymbol = this@addBackingFieldUtil.symbol
        field.parent = this@addBackingFieldUtil.parent
    }
}

inline fun <T, R> Collection<T>.memoryOptimizedMap(transform: (T) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform).compactIfPossible()
}

inline fun <T, R> Collection<T>.memoryOptimizedMapIndexed(transform: (index: Int, T) -> R): List<R> {
    return mapIndexedTo(ArrayList<R>(size), transform).compactIfPossible()
}

fun <T> List<T>.compactIfPossible(): List<T> =
    when (size) {
        0 -> emptyList()
        1 -> listOf(first())
        else -> apply {
            if (this is ArrayList<*>) trimToSize()
        }
    }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // TODO(KTIJ-26314): Remove this suppression
fun IrFactory.createExpressionBody(expression: IrExpression): IrExpressionBody =
    createExpressionBody(expression.startOffset, expression.endOffset, expression)

fun IrDeclaration.remote(): Boolean = hasAnnotation(remoteAnnotation)

fun IrDeclaration.remoteSerializable(): Boolean = hasAnnotation(remoteSerializableAnnotation)

fun IrClass.remoteConfigObject(): IrClassSymbol {
    val remoteAnnotationCall = getAnnotation(remoteAnnotation.asSingleFqName())!!
    val remoteConfigClassExpression = remoteAnnotationCall.getValueArgument(Name.identifier("context"))
        ?: error("Annotation '${remoteAnnotation.asSingleFqName().asString()}' should have an argument named `context`")
    val remoteConfigSymbol = ((remoteConfigClassExpression.type as? IrSimpleType)?.arguments[0] as? IrTypeProjection)?.type?.classOrFail
        ?: error("Cannot get RemoteConfig from type ${remoteConfigClassExpression.type}")
    return remoteConfigSymbol
}

fun RpcIrContext.irBuilder(symbol: IrSymbol): DeclarationIrBuilder =
    DeclarationIrBuilder(pluginContext, symbol, symbol.owner.startOffset, symbol.owner.endOffset)

fun IrFunction.valueParameters(): List<IrValueParameter> =
    parameters.filter { it.kind == IrParameterKind.Regular }

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

fun IrBuilder.remoteConfigContextCall(declaration: IrDeclaration, context: RpcIrContext): IrCall {
    val remoteConfigSymbol = declaration.remoteConfigObject()
    return irCall(context.remoteConfigContext.owner.getter!!.symbol).apply {
        arguments[0] = irGetObjectValue(remoteConfigSymbol.defaultType, remoteConfigSymbol)
    }
}

fun IrFunction.remoteFunctionName(): String = fqNameWhenAvailable?.asString()
    ?: error("Remote function `${name.asString()}` doesn't have fully qualified name")

fun IrBuilderWithScope.irSafeAs(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.SAFE_CAST, type, argument)

fun IrFunction.nonStaticParameters(ctx: RpcIrContext): List<IrValueParameter> = parameters.filter {
    !it.isRemoteContext(ctx)
}

fun IrValueParameter.isRemoteContext(ctx: RpcIrContext) = kind == IrParameterKind.Context && type.isSubtypeOfClass(ctx.remoteContext)