/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlinx.network.ktor

import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import network.serialization.rpcInternalKClass
import org.jetbrains.kotlinx.CallableMap
import org.jetbrains.kotlinx.network.RemoteCall
import org.jetbrains.kotlinx.network.RemoteServerImpl

@KtorDsl
fun Route.remote(path: String, builder: suspend KRemoteRoute.() -> Unit = {}) {
    post(path) {
        val remoteCall = call.receive<RemoteCall>()
        call.respond(
            RemoteServerImpl.handleCall(remoteCall),
            TypeInfo(
                CallableMap[remoteCall.callableName].returnType.kType.rpcInternalKClass<Any>(),
                CallableMap[remoteCall.callableName].returnType.kType
            )
        )
    }
}
