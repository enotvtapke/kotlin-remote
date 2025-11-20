/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.rpc.codegen.test.runners

import kotlinx.rpc.codegen.test.services.ExtensionRegistrarConfigurator
import kotlinx.rpc.codegen.test.services.RpcCompileClasspathProvider
import kotlinx.rpc.codegen.test.services.RpcRuntimeClasspathProvider
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractBoxTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            /*
             * Containers of different directives, which can be used in tests:
             * - ModuleStructureDirectives
             * - LanguageSettingsDirectives
             * - DiagnosticsDirectives
             * - FirDiagnosticsDirectives
             * - CodegenTestDirectives
             * - JvmEnvironmentConfigurationDirectives
             *
             * All of them are located in `org.jetbrains.kotlin.test.directives` package
             */
            defaultDirectives {
                +DUMP_IR
                +WITH_STDLIB
                +WITH_REFLECT
                LanguageSettingsDirectives.LANGUAGE with "+${LanguageFeature.ContextParameters.name}"
            }

            commonFirWithPluginFrontendConfiguration()
        }
    }
}

private fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration() {
    defaultDirectives {
        +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
        +FirDiagnosticsDirectives.FIR_DUMP
        +JvmEnvironmentConfigurationDirectives.FULL_JDK
        +CodegenTestDirectives.IGNORE_DEXING
    }

    useConfigurators(
        ::RpcCompileClasspathProvider,
        ::ExtensionRegistrarConfigurator,
    )

    useCustomRuntimeClasspathProviders(
        ::RpcRuntimeClasspathProvider,
    )
}