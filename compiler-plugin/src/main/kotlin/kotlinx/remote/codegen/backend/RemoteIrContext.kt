/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.nestedClasses
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class RemoteIrContext(
    val pluginContext: IrPluginContext,
) {
    val irBuiltIns = pluginContext.irBuiltIns

    val anyNullable by lazy {
        irBuiltIns.anyType.makeNullable()
    }

    val arrayOfAnyNullable by lazy {
        irBuiltIns.arrayClass.typeWith(anyNullable, Variance.OUT_VARIANCE)
    }

    val kTypeClass by lazy {
        getIrClassSymbol("kotlin.reflect", "KType")
    }

    val suspendFunction1 by lazy {
        getIrClassSymbol("kotlin.coroutines", "SuspendFunction1")
    }

    val flow by lazy {
        getIrClassSymbol("kotlinx.coroutines.flow", "Flow")
    }

    val pair by lazy {
        getIrClassSymbol("kotlin", "Pair")
    }

    val stub by lazy {
        getRemoteIrClassSymbol("Stub", "classes")
    }

    val remoteContext by lazy {
        getRemoteIrClassSymbol("RemoteContext")
    }

    val remoteSerializer by lazy {
        getRemoteIrClassSymbol("RemoteSerializer", "classes")
    }

    val remoteCall by lazy {
        getRemoteIrClassSymbol("RemoteCall", "network")
    }

    val remoteResponse by lazy {
        getRemoteIrClassSymbol("RemoteResponse", "network")
    }

    val remoteCallable by lazy {
        getRemoteIrClassSymbol("RemoteCallable")
    }

    val remoteType by lazy {
        getRemoteIrClassSymbol("RemoteType")
    }

    val remoteParameter by lazy {
        getRemoteIrClassSymbol("RemoteParameter")
    }

    val remoteConfig by lazy {
        getRemoteIrClassSymbol("RemoteConfig")
    }

    val remoteInvokator by lazy {
        getRemoteIrClassSymbol("RemoteInvokator")
    }

    val remoteConfigClient by lazy {
        remoteConfig.property("client")
    }

    val remoteConfigContext by lazy {
        remoteConfig.property("context")
    }

    val functions = Functions()

    inner class Functions {
        val mapOf by lazy {
            namedFunction("kotlin.collections", "mapOf") { f ->
                f.owner.parameters.singleOrNull { it.kind == IrParameterKind.Regular }?.isVararg ?: false
            }
        }

        val remoteClientCall by lazy {
            namedFunction("kotlinx.remote.network", "call")
        }

        val remoteClientCallStreaming by lazy {
            namedFunction("kotlinx.remote.network", "callStreaming")
        }

        val genCallableMap by lazy {
            namedFunction("kotlinx.remote", "genCallableMap")
        }

        val println by lazy {
            namedFunction("kotlin.io", "println")
        }

        val typeOf by lazy {
            namedFunction("kotlin.reflect", "typeOf")
        }

        val emptyArray by lazy {
            namedFunction("kotlin", "emptyArray")
        }

        val arrayGet by lazy {
            irBuiltIns.arrayClass.namedFunction("get")
        }

        val emptyMap by lazy {
            namedFunction("kotlin.collections", "emptyMap")
        }

        val to by lazy {
            namedFunction("kotlin", "to")
        }

        private fun IrClassSymbol.namedFunction(name: String): IrSimpleFunction {
            return owner.functions.single { it.name.asString() == name }
        }

        private fun namedFunction(
            packageName: String,
            name: String,
            filterOverloads: ((IrSimpleFunctionSymbol) -> Boolean)? = null,
        ): IrSimpleFunctionSymbol {
            val found = pluginContext.referenceFunctions(
                CallableId(
                    FqName(packageName),
                    Name.identifier(name),
                )
            )

            return if (filterOverloads == null) found.first() else found.first(filterOverloads)
        }
    }

    val properties = Properties()

    inner class Properties {
        val stubId by lazy {
            stub.namedProperty("id")
        }

        private fun IrClassSymbol.namedProperty(name: String): IrPropertySymbol {
            return owner.properties.single { it.name.asString() == name }.symbol
        }
    }

    private fun IrClassSymbol.subClass(name: String): IrClassSymbol {
        return owner.nestedClasses.singleOrNull { it.name.asString() == name }?.symbol
            ?: error("Unable to find nested class `$name` of class `${this.owner.name}`")
    }

    private fun IrClassSymbol.property(name: String): IrPropertySymbol {
        return owner.properties.singleOrNull { it.name.asString() == name }?.symbol
            ?: error("Unable to find property `$name` of class `${this.owner.name}`")
    }

    private fun getRemoteIrClassSymbol(name: String, subpackage: String? = null): IrClassSymbol {
        val suffix = subpackage?.let { ".$subpackage" } ?: ""
        return getIrClassSymbol("kotlinx.remote$suffix", name)
    }

    private fun getIrClassSymbol(packageName: String, name: String): IrClassSymbol {
        return pluginContext.referenceClass(
            ClassId(
                FqName(packageName),
                FqName(name),
                false
            )
        ) ?: error("Unable to find symbol. Package: $packageName, name: $name")
    }
}
