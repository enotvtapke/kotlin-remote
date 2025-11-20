/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.rpc.codegen.test

import kotlinx.rpc.codegen.test.runners.AbstractBoxTest
import kotlinx.rpc.codegen.test.runners.AbstractDiagnosticTest
import kotlinx.rpc.codegen.test.runners.AbstractColoringBoxTest
import kotlinx.rpc.codegen.test.runners.AbstractColoringDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "/home/enotvtapke/work/kotlin-remote/compiler-plugin-tests/src/testData", testsRoot = "src/test-gen") {
//            testClass<AbstractDiagnosticTest> {
//                model("diagnostics")
//            }

            testClass<AbstractBoxTest> {
                model("box")
            }

            testClass<AbstractColoringBoxTest> {
                model("coloringBox")
            }

            testClass<AbstractColoringDiagnosticTest> {
                model("coloringDiagnostics")
            }
        }
    }
}
