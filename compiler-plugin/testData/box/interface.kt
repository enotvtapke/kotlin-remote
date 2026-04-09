package box/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ServerConfig
import kotlinx.remote.asContext

interface Calculator {
    context(ctx: RemoteContext<RemoteConfig>)
    suspend fun multiply(x: Long): Long
}

class CalculatorStub(): Calculator {
    @Remote
    context(ctx: RemoteContext<RemoteConfig>)
    override suspend fun multiply(x: Long): Long {
        return x
    }
}

fun box(): String = runBlocking {
    context(ServerConfig.asContext()) {
        val c = CalculatorStub()
        val test1 = c.multiply(5)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
