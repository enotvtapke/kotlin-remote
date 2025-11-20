/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// RUN_PIPELINE_TILL: FRONTEND

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.annotations.Remote
import kotlinx.rpc.codegen.test.ServerConfig
import kotlinx.rpc.codegen.test.ClientContext

data class TestData(val value: String)

@Remote(ServerConfig::class)
open class BoxService {
    <!PUBLIC_FIELD_IN_RPC_SERVICE!>val x: String = "x"<!>
    <!PUBLIC_FIELD_IN_RPC_SERVICE!>internal val y: String = "y"<!>
    open suspend fun test1(testData: TestData): String = ""

    open suspend fun test2(testData: TestData): String = ""
}

fun test() = runBlocking {
    context (ClientContext) {
        val box = BoxService()

        val test1 = box.test1(TestData("value"))
        val test2 = box.test2(TestData("value"))
    }
}
