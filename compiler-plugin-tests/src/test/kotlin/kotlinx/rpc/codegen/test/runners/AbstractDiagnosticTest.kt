/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.rpc.codegen.test.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.initIdeaConfiguration
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.junit.jupiter.api.BeforeAll

open class AbstractDiagnosticTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            initIdeaConfiguration()
        }
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            defaultDirectives {
                /*
                 * Containers of different directives, which can be used in tests:
                 * - ModuleStructureDirectives
                 * - LanguageSettingsDirectives
                 * - DiagnosticsDirectives
                 * - FirDiagnosticsDirectives
                 *
                 * All of them are located in `org.jetbrains.kotlin.test.directives` package
                 */
            }

            commonFirWithPluginFrontendConfiguration()
        }
    }
}
