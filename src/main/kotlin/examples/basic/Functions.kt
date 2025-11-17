package org.jetbrains.kotlinx.examples.basic

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.kotlinx.Remote
import org.jetbrains.kotlinx.RemoteContext
import org.jetbrains.kotlinx.network.RemoteCall
import org.jetbrains.kotlinx.network.call
import org.jetbrains.kotlinx.network.callStreaming

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) =
    if (ctx == ServerConfig.context) {
        lhs * rhs
    } else {
        ServerConfig.remoteClient.call<Long>(
            RemoteCall("multiply", arrayOf(lhs, rhs))
        )
    }


@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiplyStreaming(lhs: Long, rhs: Long): Flow<Long> {
    if (ctx == ServerConfig.context) {
        return flow {
            repeat(100) {
                delay(10000)
                emit(lhs * rhs)
            }
        }
    } else {
        return ServerConfig.remoteClient.callStreaming<Long>(RemoteCall("multiplyStreaming", arrayOf(lhs, rhs)))
    }
}