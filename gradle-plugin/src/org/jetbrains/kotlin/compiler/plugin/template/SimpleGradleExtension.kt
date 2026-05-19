package org.jetbrains.kotlin.compiler.plugin.template

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class SimpleGradleExtension(objectFactory: ObjectFactory) {
    /**
     * When set, the Kotlin Remote compiler plugin runs in line-report mode:
     * it skips its usual code-generating transformations and instead writes a
     * JSON report of framework-specific source lines to the given path.
     *
     * If the path ends with `.json`, that exact file is written. Otherwise the
     * path is treated as a directory and one report file per Kotlin module is
     * written under it.
     */
    val lineReport: RegularFileProperty = objectFactory.fileProperty()

    /**
     * When line-report mode is active the plugin does not need to inject the
     * core kotlinx-remote runtime into compilations. Setting this flag to
     * `true` disables that injection so that the plugin can be applied to
     * projects that intentionally don't depend on kotlinx-remote (for example,
     * Kotlin RPC-only modules that are being measured for comparison).
     */
    val skipRuntimeInjection: Property<Boolean> = objectFactory.property(Boolean::class.javaObjectType)
}
