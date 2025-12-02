/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.test

import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.RemoteResponse

object TestRpcClient : RemoteClient {
    override suspend fun call(
        call: RemoteCall,
        returnType: TypeInfo
    ): Any? {
        return RemoteResponse.Success(42L)
    }

    override fun callStreaming(
        call: RemoteCall,
        returnType: TypeInfo
    ): Flow<Any?> {
        return flow { emit(42L) }
    }
}
