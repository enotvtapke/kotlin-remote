package org.jetbrains.kotlinx

import org.jetbrains.kotlinx.network.RemoteClient
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class Remote(val config: KClass<out RemoteConfig>)

interface RemoteContext

interface RemoteConfig {
    val context: RemoteContext
    val remoteClient: RemoteClient
}