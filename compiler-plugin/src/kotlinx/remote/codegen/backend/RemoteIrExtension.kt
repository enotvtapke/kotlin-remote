/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.backend

import kotlinx.remote.codegen.common.RemoteClassId
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class RemoteIrExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // If kotlinx-remote is not on the classpath of this compilation, there is nothing
        // for us to transform. Skip silently. This makes the plugin safe to apply to
        // projects that intentionally don't depend on kotlinx-remote (e.g. Kotlin RPC
        // projects added to the line-report comparison).
        if (pluginContext.referenceClass(RemoteClassId.remoteAnnotation) == null) return

        val context = RemoteIrContext(pluginContext)
        val scanner = RemoteFunctionScanner()
        scanner.visitModuleFragment(moduleFragment)
        CallableMapInjector(context, CallableMapGenerator(context, scanner.remoteFunctions)).visitModuleFragment(
            moduleFragment
        )
        moduleFragment.transform(RemoteFunctionBodyTransformer(), context)
        moduleFragment.transform(RemoteClassStatusTransformer(), context)
        val classScanner = RemoteClassScanner()
        classScanner.visitModuleFragment(moduleFragment)
        val remoteClassInitializer = RemoteClassInitializer(context)
        classScanner.remoteClasses.forEach { remoteClassInitializer.init(it) }
        RemoteClassListInjector(context, RemoteClassListGenerator(context, classScanner.remoteClasses)).visitModuleFragment(
            moduleFragment
        )
    }
}
