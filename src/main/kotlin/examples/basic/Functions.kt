package org.jetbrains.kotlinx.examples.basic

import org.jetbrains.kotlinx.Remote
import org.jetbrains.kotlinx.RemoteContext
import org.jetbrains.kotlinx.network.RemoteCall
import org.jetbrains.kotlinx.network.call

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long): Long {
    if (ctx == ServerConfig.context) {
        return lhs * rhs
    } else {
        return ServerConfig.remoteClient.call<Long>(RemoteCall("multiply", arrayOf(lhs, rhs)))
    }
}