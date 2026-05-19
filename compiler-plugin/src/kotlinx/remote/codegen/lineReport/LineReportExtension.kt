/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.lineReport

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

/**
 * IR extension that collects framework-specific line counts and writes a JSON
 * report. Activation is gated by a non-null output path passed via the compiler
 * option `lineReport=<file>` (see [SimpleCommandLineProcessor]) or via the
 * system property `kotlinxRemote.lineReport`.
 *
 * The report is written once per compilation (per module). When several
 * compilations share the same output path, the latest writer wins. For
 * multi-module measurement, prefer one report file per module and aggregate
 * externally.
 */
internal class LineReportExtension(
    private val outputPath: String,
    private val moduleName: String?,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val collector = LineReportCollector()
        for (file in moduleFragment.files) {
            file.accept(collector, null)
        }

        val reportFile = resolveOutputFile(outputPath, moduleName ?: moduleFragment.name.asString())
        val meta = buildMap {
            put("moduleName", moduleName ?: moduleFragment.name.asString())
            put("schema", "kotlin-remote-line-report/1")
        }
        try {
            collector.report.writeTo(reportFile, meta)
        } catch (e: Throwable) {
            System.err.println("[kotlinx-remote] failed to write line report to $reportFile: ${e.message}")
        }
    }

    private fun resolveOutputFile(path: String, moduleName: String): File {
        val asFile = File(path)
        if (path.endsWith(".json", ignoreCase = true)) return asFile
        // Treat path as a directory; emit `<dir>/<sanitized-module>.json`.
        val safeModule = moduleName.replace('/', '_').replace(':', '_').trim('<', '>').ifEmpty { "module" }
        return File(asFile, "$safeModule.json")
    }

    companion object {
        const val SYSTEM_PROPERTY: String = "kotlinxRemote.lineReport"

        fun resolveOutputPath(
            optionPath: String?,
            systemPropertyPath: String? = System.getProperty(SYSTEM_PROPERTY),
        ): String? {
            return optionPath?.takeIf { it.isNotBlank() }
                ?: systemPropertyPath?.takeIf { it.isNotBlank() }
        }
    }
}
