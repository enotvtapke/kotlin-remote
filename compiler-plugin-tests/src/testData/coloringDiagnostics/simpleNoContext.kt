/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

// RUN_PIPELINE_TILL: FRONTEND

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.annotations.Remote
import kotlinx.rpc.codegen.test.ServerConfig

data class TestData(val value: String)

@Remote(ServerConfig::class)
open class BoxService {
    open suspend fun test1(testData: TestData): String = ""

    open suspend fun test2(testData: TestData): String = ""
}

fun a() = runBlocking {
    val box = <!INVALID_REMOTE_CALL_CONTEXT!>BoxService()<!>

    val test1 = <!INVALID_REMOTE_CALL_CONTEXT!>box.test1(TestData("value"))<!>
    val test2 = <!INVALID_REMOTE_CALL_CONTEXT!>box.test2(TestData("value"))<!>
}
