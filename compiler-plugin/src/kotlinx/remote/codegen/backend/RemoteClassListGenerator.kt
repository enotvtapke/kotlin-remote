package kotlinx.remote.codegen.backend

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class RemoteClassListGenerator(
    private val ctx: RemoteIrContext,
    private val remoteClasses: MutableList<RemoteClass>
) {

    fun generate(parent: IrDeclarationParent): IrExpression {
        val descriptorType = ctx.remoteClassDescriptor.typeWith(ctx.irBuiltIns.anyType)
        val listType = ctx.irBuiltIns.listClass.typeWith(descriptorType, Variance.OUT_VARIANCE)

        if (remoteClasses.isEmpty()) {
            return IrCallImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = listType,
                symbol = ctx.functions.emptyList,
                typeArgumentsCount = 1,
            ).apply {
                typeArguments[0] = descriptorType
            }
        }

        val varargType = ctx.irBuiltIns.arrayClass.typeWith(descriptorType, Variance.OUT_VARIANCE)

        return IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = listType,
            symbol = ctx.functions.listOf,
            typeArgumentsCount = 1,
        ).apply {
            typeArguments[0] = descriptorType
            arguments[0] = IrVarargImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = varargType,
                varargElementType = descriptorType,
                elements = remoteClasses.memoryOptimizedMap { remoteClass ->
                    generateDescriptor(parent, remoteClass, descriptorType)
                }
            )
        }
    }

    private fun generateDescriptor(
        parent: IrDeclarationParent,
        remoteClass: RemoteClass,
        descriptorType: IrType,
    ): IrExpression {
        return IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = descriptorType,
            symbol = ctx.remoteClassDescriptor.constructors.single(),
            typeArgumentsCount = 1,
            constructorTypeArgumentsCount = 0,
        ).apply {
            typeArguments[0] = ctx.irBuiltIns.anyType
            arguments[0] = generateClassReference(remoteClass)
            arguments[1] = IrConstImpl.string(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                ctx.irBuiltIns.stringType,
                remoteClass.declaration.fqNameWhenAvailable?.asString() ?: remoteClass.declaration.name.asString(),
            )
            arguments[2] = generateStubFactory(parent, remoteClass)
        }
    }

    private fun generateClassReference(remoteClass: RemoteClass): IrExpression {
        val kClassAnyType = ctx.kClass.typeWith(ctx.irBuiltIns.anyType, Variance.INVARIANT)
        val classType = remoteClass.declaration.defaultType
        val kClassType = ctx.kClass.typeWith(classType, Variance.INVARIANT)
        val classReference = IrClassReferenceImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = kClassType,
            symbol = remoteClass.declaration.symbol,
            classType = classType,
        )
        return IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = kClassAnyType,
            operator = IrTypeOperator.CAST,
            typeOperand = kClassAnyType,
            argument = classReference,
        )
    }

    private fun generateStubFactory(parent: IrDeclarationParent, remoteClass: RemoteClass): IrExpression {
        val stubConstructor = remoteClass.stub.constructors.firstOrNull { it.isPrimary }
            ?: error("No primary constructor found for stub class ${remoteClass.stub.name}")

        val functionLambda = remoteClass.stub.factory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
            returnType = ctx.irBuiltIns.anyType
        }.apply {
            this.parent = parent

            val idParameter = addValueParameter {
                name = Name.identifier("id")
                type = ctx.irBuiltIns.longType
            }

            val urlParameter = addValueParameter {
                name = Name.identifier("url")
                type = ctx.irBuiltIns.stringType
            }

            body = ctx.irBuilder(symbol).irBlockBody {
                +irReturn(
                    irCallConstructor(stubConstructor.symbol, listOf()).apply {
                        arguments[0] = irGet(idParameter)
                        arguments[1] = irGet(urlParameter)
                    }
                )
            }
        }

        return IrFunctionExpressionImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = ctx.function2.typeWith(
                ctx.irBuiltIns.longType,
                ctx.irBuiltIns.stringType,
                ctx.irBuiltIns.anyType,
            ),
            origin = IrStatementOrigin.LAMBDA,
            function = functionLambda,
        )
    }
}

