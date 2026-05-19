/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.lineReport

import java.io.File

/**
 * In-memory accumulator for "framework-specific" source lines. Lines are 1-based.
 *
 * The semantics of this collector are deliberately simple: every line is a
 * (file, line) pair tagged with one or more category labels. A line can be
 * tagged by several categories (e.g. a single line that contains both an
 * `@Remote` annotation and the function signature). For aggregate counts we
 * deduplicate per file, so the same physical line counts once regardless of
 * how many tags it has.
 */
internal class LineReport {
    private val byFile = sortedMapOf<String, FileLineSet>()

    fun add(filePath: String, line: Int, category: String) {
        if (line <= 0) return
        byFile.getOrPut(filePath) { FileLineSet() }.add(line, category)
    }

    fun add(filePath: String, lines: IntRange, category: String) {
        if (lines.isEmpty()) return
        val set = byFile.getOrPut(filePath) { FileLineSet() }
        for (line in lines) {
            if (line > 0) set.add(line, category)
        }
    }

    fun isEmpty(): Boolean = byFile.values.all { it.lines.isEmpty() }

    fun toJson(meta: Map<String, String> = emptyMap()): String {
        val sb = StringBuilder()
        sb.append("{\n")
        for ((k, v) in meta) {
            sb.append("  ").append(jsonString(k)).append(": ").append(jsonString(v)).append(",\n")
        }

        val totalLines = byFile.values.sumOf { it.lines.size }
        val categoryTotals = sortedMapOf<String, Int>()
        for (set in byFile.values) {
            for (cats in set.lines.values) {
                for (cat in cats) categoryTotals.merge(cat, 1) { a, b -> a + b }
            }
        }

        sb.append("  \"totalFrameworkLines\": ").append(totalLines).append(",\n")
        sb.append("  \"categoryTotals\": {")
        var firstCat = true
        for ((cat, count) in categoryTotals) {
            if (!firstCat) sb.append(",")
            firstCat = false
            sb.append("\n    ").append(jsonString(cat)).append(": ").append(count)
        }
        if (!firstCat) sb.append("\n  ")
        sb.append("},\n")

        sb.append("  \"files\": [")
        var firstFile = true
        for ((path, set) in byFile) {
            if (!firstFile) sb.append(",")
            firstFile = false
            sb.append("\n    {\n")
            sb.append("      \"path\": ").append(jsonString(path)).append(",\n")
            sb.append("      \"frameworkLines\": ").append(set.lines.size).append(",\n")
            val perCategory = sortedMapOf<String, Int>()
            for (cats in set.lines.values) for (cat in cats) perCategory.merge(cat, 1) { a, b -> a + b }
            sb.append("      \"categoryTotals\": {")
            var firstC = true
            for ((cat, count) in perCategory) {
                if (!firstC) sb.append(",")
                firstC = false
                sb.append("\n        ").append(jsonString(cat)).append(": ").append(count)
            }
            if (!firstC) sb.append("\n      ")
            sb.append("},\n")
            sb.append("      \"lines\": [")
            val lineList = set.lines.keys.sorted()
            for ((idx, line) in lineList.withIndex()) {
                if (idx > 0) sb.append(", ")
                sb.append(line)
            }
            sb.append("]\n    }")
        }
        if (!firstFile) sb.append("\n  ")
        sb.append("]\n}\n")

        return sb.toString()
    }

    fun writeTo(file: File, meta: Map<String, String> = emptyMap()) {
        file.parentFile?.mkdirs()
        file.writeText(toJson(meta))
    }

    private fun jsonString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) {
                    sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private class FileLineSet {
        val lines: MutableMap<Int, MutableSet<String>> = sortedMapOf()
        fun add(line: Int, category: String) {
            lines.getOrPut(line) { mutableSetOf() }.add(category)
        }
    }
}
