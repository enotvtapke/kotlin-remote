package kotlinx.remote.codegen.backend

import kotlinx.remote.codegen.common.RemoteNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.*
import kotlin.properties.Delegates

class RemoteClassInitializer(private val ctx: RemoteIrContext) {
    fun init(remoteClass: RemoteClass) {
        initStubClass(remoteClass.stub, remoteClass.declaration)
    }

    private var stubConstructor: IrConstructor by Delegates.notNull()

    private fun initStubClass(stubClass: IrClass, remoteClass: IrClass) {
        stubClass.declarations.removeAll { declaration ->
            declaration.isFakeOverride && (declaration.getNameWithAssert() == ctx.properties.stubId.owner.name ||
                declaration.getNameWithAssert() == ctx.properties.stubUrl.owner.name)
        }

        val existingConstructor = stubClass.constructors.firstOrNull { it.isPrimary } ?: error("Class ${remoteClass.name} has no primary constructor")
        stubConstructor = existingConstructor
        existingConstructor.body = ctx.irBuilder(existingConstructor.symbol).irBlockBody {
            +irDelegatingConstructorCall(remoteClass.constructors.firstOrNull { constructor ->
                constructor.parameters.all { it.defaultValue != null }
            } ?: error("Class ${remoteClass.name} has no default constructor"))
            +IrInstanceInitializerCallImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                classSymbol = stubClass.symbol,
                type = context.irBuiltIns.unitType,
            )
        }
        val stubId = existingConstructor.parameters.firstOrNull { it.name == ctx.properties.stubId.owner.name }
            ?: error("FIR-generated constructor missing id parameter")
        val stubUrl = existingConstructor.parameters.firstOrNull { it.name == ctx.properties.stubUrl.owner.name }
            ?: error("FIR-generated constructor missing url parameter")

        stubClass.addProperty {
            name = ctx.properties.stubId.owner.name
        }.apply {
            addBackingField {
                visibility = PRIVATE
                type = ctx.irBuiltIns.longType
                isFinal = true
                overriddenSymbols = listOf(ctx.properties.stubId)
            }.apply {
                initializer = factory.createExpressionBody(
                    IrGetValueImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = stubId.type,
                        symbol = stubId.symbol,
                        origin = IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER,
                    )
                )
            }
            addDefaultGetter(stubClass, ctx.irBuiltIns) {
                overriddenSymbols = listOf(ctx.properties.stubId.owner.getterOrFail.symbol)
            }
        }

        stubClass.addProperty {
            name = ctx.properties.stubUrl.owner.name
        }.apply {
            addBackingField {
                visibility = PRIVATE
                type = ctx.irBuiltIns.stringType
                isFinal = true
                overriddenSymbols = listOf(ctx.properties.stubUrl)
            }.apply {
                initializer = factory.createExpressionBody(
                    IrGetValueImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = stubUrl.type,
                        symbol = stubUrl.symbol,
                        origin = IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER,
                    )
                )
            }
            addDefaultGetter(stubClass, ctx.irBuiltIns) {
                overriddenSymbols = listOf(ctx.properties.stubUrl.owner.getterOrFail.symbol)
            }
        }
    }
}