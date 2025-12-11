package box/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.genCallableMap
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ServerContext

@Remote
context(ctx: RemoteContext)
suspend fun Long.multiply(rhs: Long) = this * rhs

fun box(): String = runBlocking {
    context(ServerContext) {
        val test1 = 5L.multiply(6)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
