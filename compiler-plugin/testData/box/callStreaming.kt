package box/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.remoteClient
import kotlinx.remote.codegen.test.ServerConfig
import kotlinx.remote.codegen.test.ClientContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
fun multiplyStreaming(lhs: Long, rhs: Long): Flow<Long> {
    return flow {
        repeat(50) {
            emit(lhs * rhs)
        }
    }
}

fun box(): String = runBlocking {
    context(ClientContext) {
        val test1 = multiplyStreaming(5, 6).single()
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
