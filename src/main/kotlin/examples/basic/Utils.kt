package org.jetbrains.kotlinx.examples.basic

import org.jetbrains.kotlinx.RemoteConfig
import org.jetbrains.kotlinx.RemoteContext
import org.jetbrains.kotlinx.network.RemoteClient
import org.jetbrains.kotlinx.network.remoteClient

data object ServerConfig : RemoteConfig {
    override val context = ServerContext
    override val remoteClient: RemoteClient = remoteClient("http://localhost:8080", "/call")
}

data object ServerContext : RemoteContext
