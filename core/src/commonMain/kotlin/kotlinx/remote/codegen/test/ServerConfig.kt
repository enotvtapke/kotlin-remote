/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.test

import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.WrappedRemote

data object ServerRemoteContext: RemoteContext {
    override val client: RemoteClient = TestRpcClient
}

val ServerContext = WrappedRemote(ServerRemoteContext)
