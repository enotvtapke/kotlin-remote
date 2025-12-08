package box/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ClientContext

@RemoteSerializable
class Calculator(private var init: Long = 0) {
    @Remote
    context(ctx: RemoteContext)
    suspend fun multiply(x: Long): Long {
        init *= x
        return init
    }
}

fun box(): String = runBlocking {
    context(ClientContext) {
        val c = Calculator(1)
        val test1 = c.multiply(5)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
