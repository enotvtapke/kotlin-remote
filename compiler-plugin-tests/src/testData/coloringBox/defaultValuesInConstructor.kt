/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.annotations.Remote
import kotlinx.rpc.codegen.test.ServerConfig
import kotlinx.rpc.codegen.test.ClientContext

data class TestData(val value: String)

@Remote(ServerConfig::class)
open class BoxService(first: Int = -1, counter: Int = 1) {

    open suspend fun test1(testData: TestData): String = ""

    open suspend fun test2(testData: TestData): String = ""
}

fun box(): String = runBlocking {
    context(ClientContext) {
        val box = BoxService(counter = 0)
        val test1 = box.test1(TestData("value"))
        val test2 = box.test2(TestData("value"))

        if (test1 == "call_42" && test2 == "call_42") "OK" else "Fail: test1=$test1, test2=$test2"
    }
}
