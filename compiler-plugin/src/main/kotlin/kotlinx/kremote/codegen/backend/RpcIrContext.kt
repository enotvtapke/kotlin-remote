/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.kremote.codegen.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.nestedClasses
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.types.Variance

class RpcIrContext(
    val pluginContext: IrPluginContext,
) {
    val irBuiltIns = pluginContext.irBuiltIns

    val anyNullable by lazy {
        irBuiltIns.anyType.makeNullable()
    }

    val arrayOfAnyNullable by lazy {
        irBuiltIns.arrayClass.typeWith(anyNullable, Variance.OUT_VARIANCE)
    }

    val listOfAnnotations by lazy {
        irBuiltIns.listClass.typeWith(irBuiltIns.annotationType)
    }

    val arrayOfAnnotations by lazy {
        irBuiltIns.arrayClass.typeWith(irBuiltIns.annotationType, Variance.OUT_VARIANCE)
    }

    val kTypeClass by lazy {
        getIrClassSymbol("kotlin.reflect", "KType")
    }

    val suspendFunction1 by lazy {
        getIrClassSymbol("kotlin.coroutines", "SuspendFunction1")
    }

    val suspendFunction2 by lazy {
        getIrClassSymbol("kotlin.coroutines", "SuspendFunction2")
    }

    val flow by lazy {
        getIrClassSymbol("kotlinx.coroutines.flow", "Flow")
    }

    val pair by lazy {
        getIrClassSymbol("kotlin", "Pair")
    }

    val encoder by lazy {
        getIrClassSymbol("kotlinx.serialization.encoding", "Encoder")
    }

    val decoder by lazy {
        getIrClassSymbol("kotlinx.serialization.encoding", "Decoder")
    }

    val serialDescriptor by lazy {
        getIrClassSymbol("kotlinx.serialization.descriptors", "SerialDescriptor")
    }

    val primitiveKindLong by lazy {
        getIrClassSymbol("kotlinx.serialization.descriptors", "PrimitiveKind.LONG")
    }

    val remoteClient by lazy {
        getRpcIrClassSymbol("RemoteClient", "network")
    }

    val remoteContext by lazy {
        getRpcIrClassSymbol("RemoteContext")
    }

    val remoteCall by lazy {
        getRpcIrClassSymbol("RemoteCall", "network")
    }

    val callableMap by lazy {
        getRpcIrClassSymbol("CallableMap")
    }

    val callableMapMap by lazy {
        callableMap.property("callableMap")
    }

    val rpcCall by lazy {
        getRpcIrClassSymbol("RpcCall")
    }

    val withServiceDescriptor by lazy {
        getRpcIrClassSymbol("WithServiceDescriptor", "internal")
    }

    val rpcServiceDescriptor by lazy {
        getRpcIrClassSymbol("RpcServiceDescriptor", "descriptor")
    }

    val rpcType by lazy {
        getRpcIrClassSymbol("RpcType", "descriptor")
    }

    val rpcTypeDefault by lazy {
        getRpcIrClassSymbol("RpcTypeDefault", "descriptor")
    }

    val rpcTypeKrpc by lazy {
        getRpcIrClassSymbol("RpcTypeKrpc", "descriptor")
    }

    val remoteCallable by lazy {
        getRpcIrClassSymbol("RemoteCallable")
    }


    val remoteType by lazy {
        getRpcIrClassSymbol("RemoteType")
    }


    val rpcCallable by lazy {
        getRpcIrClassSymbol("RpcCallable")
    }

    val rpcCallableDefault by lazy {
        getRpcIrClassSymbol("RpcCallableDefault", "descriptor")
    }

    private val rpcInvokator by lazy {
        getRpcIrClassSymbol("RpcInvokator", "descriptor")
    }

    val rpcInvokatorMethod by lazy {
        rpcInvokator.subClass("Method")
    }

    val rpcInvokatorConstructor by lazy {
        rpcInvokator.subClass("Constructor")
    }

    val rpcParameter by lazy {
        getRpcIrClassSymbol("RpcParameter", "descriptor")
    }

    val remoteParameter by lazy {
        getRpcIrClassSymbol("RemoteParameter")
    }

    val rpcParameterDefault by lazy {
        getRpcIrClassSymbol("RpcParameterDefault", "descriptor")
    }

    val remoteConfig by lazy {
        getRpcIrClassSymbol("RemoteConfig")
    }

    val remoteInvokator by lazy {
        getRpcIrClassSymbol("RemoteInvokator")
    }

    val remoteConfigClient by lazy {
        remoteConfig.property("client")
    }

    val remoteConfigContext by lazy {
        remoteConfig.property("context")
    }

    val kSerializer by lazy {
        getIrClassSymbol("kotlinx.serialization", "KSerializer")
    }

    val kSerializerAnyNullable by lazy {
        kSerializer.typeWith(anyNullable)
    }

    val kSerializerAnyNullableKClass by lazy {
        irBuiltIns.kClassClass.typeWith(kSerializerAnyNullable)
    }


    fun isNativeTarget(): Boolean {
        return pluginContext.platform.isNative()
    }


    val functions = Functions()

    inner class Functions {
        val mapOf by lazy {
            namedFunction("kotlin.collections", "mapOf") {
                    it.owner.valueParameters().singleOrNull()?.isVararg ?: false
            }
        }
//        val remoteClientCall by lazy {
//            remoteClient.namedFunction("call")
//        }

        val remoteClientCall by lazy {
            namedFunction("kotlinx.remote.network", "call")
        }

        val remoteClientCallStreaming by lazy {
            namedFunction("kotlinx.remote.network", "callStreaming")
        }

        val rpcClientCallServerStreaming by lazy {
            remoteClient.namedFunction("callServerStreaming")
        }

        val callableMapInit by lazy {
            callableMap.namedFunction("init")
        }

        val rpcClientCloseService by lazy {
            remoteClient.namedFunction("closeService")
        }

        val encoderEncodeLong by lazy {
            encoder.namedFunction("encodeLong")
        }

        val decoderDecodeLong by lazy {
            decoder.namedFunction("decodeLong")
        }

        val primitiveSerialDescriptor by lazy {
            namedFunction("kotlinx.serialization.descriptors", "PrimitiveSerialDescriptor")
        }

        val rpcClientWithService by lazy {
            namedFunction("kotlinx.rpc", "withService") {
                it.owner.parameters.count() == 1
            }
        }

        val println by lazy {
            namedFunction("kotlin.io", "println")
        }

        val registerRemoteService by lazy {
            namedFunction("kotlinx.rpc", "registerRemoteService")
        }

        val typeOf by lazy {
            namedFunction("kotlin.reflect", "typeOf")
        }

        val emptyArray by lazy {
            namedFunction("kotlin", "emptyArray")
        }

        val emptyList by lazy {
            namedFunction("kotlin.collections", "emptyList")
        }

        val mapGet by lazy {
            irBuiltIns.mapClass.namedFunction("get")
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
        val rpcServiceDescriptorFqName by lazy {
            rpcServiceDescriptor.namedProperty("fqName")
        }

        val rpcServiceDescriptorSerializer by lazy {
            rpcServiceDescriptor.namedProperty("serializer")
        }

        val kSerializerDescriptor by lazy {
            kSerializer.namedProperty("descriptor")
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

    private fun getRpcIrClassSymbol(name: String, subpackage: String? = null): IrClassSymbol {
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
