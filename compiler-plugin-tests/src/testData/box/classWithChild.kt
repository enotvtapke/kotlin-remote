/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.remoteClient
import kotlinx.rpc.codegen.test.ServerConfig
import kotlinx.rpc.codegen.test.ClientContext
import kotlinx.remote.CallableMap

open class Calculator(private var init: Long) {
    @Remote(ServerConfig::class)
    context(ctx: RemoteContext)
    suspend fun multiply(x: Long): Long {
        init *= x
        return init
    }
}

class CalculatorStub(): Calculator(0)

fun box(): String = runBlocking {
    CallableMap.init()
    context(ClientContext) {
        val c = Calculator(1)
        val test1 = c.multiply(5)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
