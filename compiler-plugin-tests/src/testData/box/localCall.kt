/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.rpc.codegen.test.ServerConfig
import kotlinx.rpc.codegen.test.ServerContext

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun box(): String = runBlocking {
    context(ServerContext) {
        val test1 = multiply(5, 6)
        if (test1 == 30L) "OK" else "Fail: test1=$test1"
    }
}
