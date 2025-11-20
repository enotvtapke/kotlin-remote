/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.rpc.codegen.test.runners

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives

open class AbstractColoringDiagnosticTest : BaseTestRunner() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with "+${LanguageFeature.ContextParameters.name}"
            }

            commonFirWithPluginFrontendConfiguration()
        }
    }
}
