/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.test

import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteResponse

object TestRpcClient : RemoteClient {
    override suspend fun call(
        call: RemoteCall,
    ): Any? {
        return 42L
    }
}
