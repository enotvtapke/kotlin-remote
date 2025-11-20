/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.annotations.Remote
import kotlinx.rpc.codegen.test.ServerConfig
import kotlinx.rpc.codegen.test.ServerContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.rpc.descriptor.serviceDescriptorOf
import kotlinx.rpc.internal.utils.ExperimentalRpcApi

data class TestData(val value: String)

@Remote(ServerConfig::class)
open class BoxService {
    open suspend fun test1(testData: TestData): String = ""

    open suspend fun test2(testData: TestData): String = ""
}

@OptIn(ExperimentalRpcApi::class)
fun box(): String = runBlocking {
    context(ServerContext) {
        val box = BoxService()
        val descriptor = serviceDescriptorOf<BoxService>()
        val appModule = SerializersModule {
            contextual(descriptor.serializer!!)
        }
        val json = Json {
            serializersModule = appModule
            prettyPrint = true
        }
        val encoded = json.encodeToString(box)
        if (encoded == "1") "OK" else "Fail: encoded=$encoded"
    }
}
