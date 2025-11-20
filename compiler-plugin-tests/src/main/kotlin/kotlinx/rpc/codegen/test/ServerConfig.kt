/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.rpc.codegen.test

import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteClient

data object ServerConfig: RemoteConfig {
    override val context = ServerContext
    override val client: RemoteClient = TestRpcClient
}

data object ServerContext: RemoteContext
data object ClientContext: RemoteContext
