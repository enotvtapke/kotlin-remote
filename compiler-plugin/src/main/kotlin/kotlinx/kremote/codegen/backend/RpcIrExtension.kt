/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.kremote.codegen.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class RpcIrExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = RpcIrContext(pluginContext)
        val scanner = RemoteFunctionScanner()
        scanner.visitModuleFragment(moduleFragment)
        CallableMapInjector(context, CallableMapGenerator(context, scanner.remoteFunctions)).visitModuleFragment(
            moduleFragment
        )
        moduleFragment.transform(RemoteFunctionBodyTransformer(), context)
        moduleFragment.transform(RpcIrServiceStatusTransformer(), context)
        val classScanner = RemoteClassScanner()
        classScanner.visitModuleFragment(moduleFragment)
        val remoteClassInitializer = RemoteClassInitializer(context)
        classScanner.remoteClasses.forEach { remoteClassInitializer.init(it) }
    }
}
