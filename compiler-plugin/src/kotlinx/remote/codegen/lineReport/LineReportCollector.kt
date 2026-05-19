/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.lineReport

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * IR visitor that classifies declarations and expressions into framework-specific
 * line categories. The collector inspects only annotation FQ names and a small set
 * of well-known FQ names so it can run on Kotlin Remote *and* Kotlin RPC projects
 * without requiring the kotlinx-remote runtime to be on the classpath.
 *
 * Heuristics for "signature lines" of a declaration that has a body:
 *  - Block body (`{ ... }`): include [decl.startLine .. body.startLine]. The line
 *    with `{` is treated as the last signature line; lines strictly inside the
 *    block are body lines and are not counted.
 *  - Expression body (`= expr`): include [decl.startLine .. exprStartLine] when
 *    the body fits on the same line as `=`, and [decl.startLine .. exprStartLine - 1]
 *    when the expression is on a separate line. There is a known minor edge case
 *    when a multi-line declaration places the `=` sign on the same line as the
 *    body expression (rare in idiomatic Kotlin), in which case the body line is
 *    miscounted as signature.
 *  - No body (interface method, abstract member): include [decl.startLine .. decl.endLine].
 */
internal class LineReportCollector : IrVisitorVoid() {
    val report = LineReport()

    private var currentFilePath: String? = null
    private val fileEntries = ArrayDeque<IrFile>()
    private val enclosingFunctions = ArrayDeque<IrFunction>()
    private var contextBlockDepth: Int = 0

    /** Lazily populated cache of source lines per file path; index 0 is line 1. */
    private val sourceLinesCache = mutableMapOf<String, List<String>?>()

    private fun sourceLines(path: String): List<String>? {
        return sourceLinesCache.getOrPut(path) {
            try {
                java.io.File(path).readLines(Charsets.UTF_8)
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * For a class declaration whose body spans multiple physical lines, returns the
     * 1-based source line that contains the opening `{` of the class body. Falls back
     * to the class start line when the source file cannot be read.
     */
    private fun classHeaderEndLine(cls: IrClass): Int {
        val path = currentFilePath ?: return lineOf(cls.startOffset) ?: 1
        val startLine = lineOf(cls.startOffset) ?: return 1
        val lines = sourceLines(path) ?: return startLine
        // Walk from the class start downwards looking for the first `{` that is not
        // inside parentheses (so we ignore `(` of constructor arg lists). This is
        // approximate but works for idiomatic Kotlin.
        var depth = 0
        for (i in (startLine - 1) until lines.size) {
            val s = lines[i]
            for (c in s) {
                when (c) {
                    '(' -> depth++
                    ')' -> if (depth > 0) depth--
                    '{' -> if (depth == 0) return i + 1
                }
            }
        }
        return startLine
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        val previousPath = currentFilePath
        currentFilePath = declaration.fileEntry.name
        fileEntries.addLast(declaration)
        try {
            declaration.acceptChildrenVoid(this)
        } finally {
            fileEntries.removeLast()
            currentFilePath = previousPath
        }
    }

    override fun visitClass(declaration: IrClass) {
        val isRpcInterface = declaration.hasAnnotationFq(RPC_ANNOTATION)
        val isRemoteSerializable = declaration.hasAnnotationFq(REMOTE_SERIALIZABLE_ANNOTATION)
        val implementsRpc = !isRpcInterface && classDirectlyImplementsRpc(declaration)
        val implementsRemoteConfig = classImplementsByFq(declaration, REMOTE_CONFIG_FQN) &&
            declaration.kotlinFqName.asString() != REMOTE_CONFIG_FQN

        if (isRpcInterface) collectRpcInterface(declaration)
        if (implementsRpc) collectRpcImplClass(declaration)
        if (isRemoteSerializable) collectRemoteSerializableClass(declaration)
        if (implementsRemoteConfig) collectRemoteConfigClass(declaration)

        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val isRemote = declaration.hasAnnotationFq(REMOTE_ANNOTATION)
        if (isRemote) collectRemoteFunctionDeclaration(declaration)
        enclosingFunctions.addLast(declaration)
        try {
            declaration.acceptChildrenVoid(this)
        } finally {
            enclosingFunctions.removeLast()
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        // Anonymous functions, constructors, etc.
        enclosingFunctions.addLast(declaration)
        try {
            declaration.acceptChildrenVoid(this)
        } finally {
            enclosingFunctions.removeLast()
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        if (declaration.parent is IrFile) {
            val initializer = declaration.backingField?.initializer?.expression
            if (initializer is IrCall && initializer.calleeFqName() == AS_CONTEXT_FQN) {
                addRange(
                    rangeStart = declarationStart(declaration),
                    rangeEnd = declaration.endOffset,
                    category = CAT_KR_HELPER_CALL,
                )
            }
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        val calleeFq = expression.calleeFqName()
        val enclosingRemote = enclosingFunctions.lastOrNull { it.hasAnnotationFq(REMOTE_ANNOTATION) }
        val callerIsRemote = enclosingRemote != null
        val callerIsRpcImpl = (enclosingFunctions.lastOrNull() as? IrSimpleFunction)?.overridesRpcMethod() == true

        when (calleeFq) {
            AS_CONTEXT_FQN, RUN_WITH_FQN -> addLine(expression.startOffset, CAT_KR_HELPER_CALL)
            KOTLIN_CONTEXT_FQN, KOTLIN_WITH_FQN -> {
                if (looksLikeRemoteContextBlock(expression)) {
                    addLine(expression.startOffset, CAT_KR_CONTEXT_BLOCK)
                    addLine(expression.endOffset, CAT_KR_CONTEXT_BLOCK)
                    contextBlockDepth++
                    try {
                        expression.acceptChildrenVoid(this)
                    } finally {
                        contextBlockDepth--
                    }
                    return
                }
            }
            WITH_SERVICE_FQN, REGISTER_SERVICE_FQN -> addLine(expression.startOffset, CAT_KRPC_HELPER_CALL)
            else -> {
                val callee = expression.symbol.owner
                val calleeIsRemote = callee.hasAnnotationFq(REMOTE_ANNOTATION)
                if (calleeIsRemote && (!callerIsRemote || contextBlockDepth > 0)) {
                    addLine(expression.startOffset, CAT_KR_CALL_SITE)
                }
                if (!callerIsRpcImpl) {
                    val parent = (callee as? IrSimpleFunction)?.parent as? IrClass
                    if (parent != null && parent.hasAnnotationFq(RPC_ANNOTATION)) {
                        addLine(expression.startOffset, CAT_KRPC_CALL_SITE)
                    }
                }
            }
        }

        expression.acceptChildrenVoid(this)
    }

    // ---------------------------------------------------------------------------
    // Counting helpers
    // ---------------------------------------------------------------------------

    private fun collectRemoteFunctionDeclaration(fn: IrSimpleFunction) {
        val annotation = fn.findAnnotationFq(REMOTE_ANNOTATION)
        if (annotation != null) addLine(annotation.startOffset, CAT_KR_ANNOTATION)
        for (cp in fn.contextParameters()) {
            addRange(cp.startOffset, cp.endOffset, CAT_KR_CONTEXT_PARAM)
        }
        addRange(declarationStart(fn), signatureEndOffset(fn), CAT_KR_FUNCTION_SIGNATURE)
    }

    private fun collectRemoteSerializableClass(cls: IrClass) {
        val annotation = cls.findAnnotationFq(REMOTE_SERIALIZABLE_ANNOTATION)
        if (annotation != null) addLine(annotation.startOffset, CAT_KR_ANNOTATION)
        addLine(declarationStart(cls), CAT_KR_CONFIG_DECL)
    }

    private fun collectRemoteConfigClass(cls: IrClass) {
        addClassHeaderRange(cls, CAT_KR_CONFIG_DECL)
        addLine(cls.endOffset, CAT_KR_CONFIG_DECL)
        for (member in cls.declarations) {
            if (member is IrProperty && member.name.asString() == "client" && member.isOverride()) {
                addRange(declarationStart(member), member.endOffset, CAT_KR_CONFIG_DECL)
            }
        }
    }

    private fun collectRpcInterface(cls: IrClass) {
        val annotation = cls.findAnnotationFq(RPC_ANNOTATION)
        if (annotation != null) addLine(annotation.startOffset, CAT_KRPC_ANNOTATION)
        addClassHeaderRange(cls, CAT_KRPC_INTERFACE)
        addLine(cls.endOffset, CAT_KRPC_INTERFACE)
        for (member in cls.declarations) {
            if (member is IrSimpleFunction && !member.isFakeOverride && !isObjectMethod(member)) {
                addRange(declarationStart(member), signatureEndOffset(member), CAT_KRPC_INTERFACE)
            }
        }
    }

    private fun collectRpcImplClass(cls: IrClass) {
        addClassHeaderRange(cls, CAT_KRPC_IMPL)
        addLine(cls.endOffset, CAT_KRPC_IMPL)
        for (member in cls.declarations) {
            if (member is IrSimpleFunction && member.overridesRpcMethod()) {
                addRange(declarationStart(member), signatureEndOffset(member), CAT_KRPC_IMPL)
            }
        }
    }

    private fun addClassHeaderRange(cls: IrClass, category: String) {
        val path = currentFilePath ?: return
        val startLine = lineOf(declarationStart(cls)) ?: return
        val endLine = classHeaderEndLine(cls).coerceAtLeast(startLine)
        report.add(path, startLine..endLine, category)
    }

    // ---------------------------------------------------------------------------
    // Predicates
    // ---------------------------------------------------------------------------

    private fun classDirectlyImplementsRpc(cls: IrClass): Boolean {
        if (cls.kind == ClassKind.INTERFACE) return false
        if (cls.kind == ClassKind.ANNOTATION_CLASS) return false
        for (sup in cls.superTypes) {
            val sc = sup.classOrNull?.owner ?: continue
            if (sc.hasAnnotationFq(RPC_ANNOTATION)) return true
        }
        return false
    }

    private fun classImplementsByFq(cls: IrClass, fqn: String): Boolean {
        if (cls.kotlinFqName.asString() == fqn) return true
        val visited = mutableSetOf<IrClass>()
        val queue = ArrayDeque<IrClass>()
        queue.add(cls)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (!visited.add(cur)) continue
            for (sup in cur.superTypes) {
                val sc = sup.classOrNull?.owner ?: continue
                if (sc.kotlinFqName.asString() == fqn) return true
                queue.add(sc)
            }
        }
        return false
    }

    private fun looksLikeRemoteContextBlock(call: IrCall): Boolean {
        for (arg in call.arguments) {
            val type = arg?.type ?: continue
            if (typeRefersToRemoteContext(type)) return true
        }
        return false
    }

    private fun typeRefersToRemoteContext(type: IrType): Boolean {
        val fq = type.classFqName?.asString() ?: return false
        if (fq == REMOTE_CONTEXT_FQN || fq == CONFIGURED_CONTEXT_FQN || fq == LOCAL_CONTEXT_FQN) return true
        val cls = type.classOrNull?.owner ?: return false
        return classImplementsByFq(cls, REMOTE_CONFIG_FQN)
    }

    private fun IrSimpleFunction.overridesRpcMethod(): Boolean {
        return overriddenSymbols.any { sym ->
            val parent = sym.owner.parent as? IrClass
            parent != null && parent.hasAnnotationFq(RPC_ANNOTATION)
        }
    }

    private fun IrProperty.isOverride(): Boolean {
        val getter = getter ?: return false
        return getter.overriddenSymbols.isNotEmpty()
    }

    private fun isObjectMethod(fn: IrSimpleFunction): Boolean {
        val name = fn.name.asString()
        return name == "equals" || name == "hashCode" || name == "toString"
    }

    private fun IrCall.calleeFqName(): String? {
        return try {
            symbol.owner.kotlinFqName.asString()
        } catch (_: Throwable) {
            null
        }
    }

    // ---------------------------------------------------------------------------
    // Source range helpers
    // ---------------------------------------------------------------------------

    private fun declarationStart(decl: IrDeclaration): Int {
        var minOffset = decl.startOffset.takeIf { it != UNDEFINED_OFFSET } ?: Int.MAX_VALUE
        for (ann in (decl as? IrAnnotationContainer)?.annotations.orEmpty()) {
            if (ann.startOffset != UNDEFINED_OFFSET && ann.startOffset < minOffset) {
                minOffset = ann.startOffset
            }
        }
        if (decl is IrFunction) {
            for (cp in decl.contextParameters()) {
                if (cp.startOffset != UNDEFINED_OFFSET && cp.startOffset < minOffset) {
                    minOffset = cp.startOffset
                }
            }
        }
        return if (minOffset == Int.MAX_VALUE) decl.startOffset else minOffset
    }

    private fun signatureEndOffset(fn: IrFunction): Int {
        val body = fn.body ?: return fn.endOffset
        val bodyStart = body.startOffset
        if (bodyStart == UNDEFINED_OFFSET) return fn.endOffset
        return when (body) {
            is IrBlockBody -> bodyStart // include the `{` line
            is IrExpressionBody -> {
                val exprStart = body.expression.startOffset
                if (exprStart == UNDEFINED_OFFSET) return bodyStart
                val declStartLine = lineOf(declarationStart(fn))
                val exprStartLine = lineOf(exprStart)
                if (declStartLine != null && exprStartLine != null && exprStartLine > declStartLine) {
                    exprStart - 1 // stop just before the body expression line
                } else {
                    exprStart
                }
            }
            else -> fn.endOffset
        }
    }

    private fun addLine(offset: Int, category: String) {
        val path = currentFilePath ?: return
        if (offset == UNDEFINED_OFFSET || offset < 0) return
        val line = lineOf(offset) ?: return
        report.add(path, line, category)
    }

    private fun addRange(rangeStart: Int, rangeEnd: Int, category: String) {
        val path = currentFilePath ?: return
        if (rangeStart == UNDEFINED_OFFSET || rangeEnd == UNDEFINED_OFFSET) return
        val sLine = lineOf(rangeStart) ?: return
        val eLine = lineOf(rangeEnd) ?: return
        if (eLine < sLine) {
            report.add(path, sLine, category)
            return
        }
        report.add(path, sLine..eLine, category)
    }

    private fun lineOf(offset: Int): Int? {
        val file = fileEntries.lastOrNull() ?: return null
        if (offset == UNDEFINED_OFFSET || offset < 0) return null
        return try {
            file.fileEntry.getLineNumber(offset) + 1 // 1-based
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        const val CAT_KR_ANNOTATION = "kotlinRemote.annotation"
        const val CAT_KR_FUNCTION_SIGNATURE = "kotlinRemote.functionSignature"
        const val CAT_KR_CONTEXT_PARAM = "kotlinRemote.contextParam"
        const val CAT_KR_CONFIG_DECL = "kotlinRemote.configDecl"
        const val CAT_KR_CONTEXT_BLOCK = "kotlinRemote.contextBlock"
        const val CAT_KR_CALL_SITE = "kotlinRemote.callSite"
        const val CAT_KR_HELPER_CALL = "kotlinRemote.helperCall"

        const val CAT_KRPC_ANNOTATION = "kotlinRpc.annotation"
        const val CAT_KRPC_INTERFACE = "kotlinRpc.interface"
        const val CAT_KRPC_IMPL = "kotlinRpc.implClass"
        const val CAT_KRPC_HELPER_CALL = "kotlinRpc.helperCall"
        const val CAT_KRPC_CALL_SITE = "kotlinRpc.callSite"

        const val REMOTE_ANNOTATION = "kotlinx.remote.Remote"
        const val REMOTE_SERIALIZABLE_ANNOTATION = "kotlinx.remote.classes.RemoteSerializable"
        const val REMOTE_CONFIG_FQN = "kotlinx.remote.RemoteConfig"
        const val REMOTE_CONTEXT_FQN = "kotlinx.remote.RemoteContext"
        const val CONFIGURED_CONTEXT_FQN = "kotlinx.remote.ConfiguredContext"
        const val LOCAL_CONTEXT_FQN = "kotlinx.remote.LocalContext"
        const val AS_CONTEXT_FQN = "kotlinx.remote.asContext"
        const val RUN_WITH_FQN = "kotlinx.remote.runWith"
        const val KOTLIN_CONTEXT_FQN = "kotlin.context"
        const val KOTLIN_WITH_FQN = "kotlin.with"

        const val RPC_ANNOTATION = "kotlinx.rpc.annotations.Rpc"
        const val WITH_SERVICE_FQN = "kotlinx.rpc.withService"
        const val REGISTER_SERVICE_FQN = "kotlinx.rpc.registerService"
    }
}

// -------------------------------------------------------------------------------
// Top-level helpers (extension methods on IR types).
// -------------------------------------------------------------------------------

private fun IrDeclaration.hasAnnotationFq(fqn: String): Boolean {
    val container = this as? IrAnnotationContainer ?: return false
    return container.annotations.any { it.constructorClassFqName() == fqn }
}

private fun IrDeclaration.findAnnotationFq(fqn: String): IrConstructorCall? {
    val container = this as? IrAnnotationContainer ?: return null
    return container.annotations.firstOrNull { it.constructorClassFqName() == fqn }
}

private fun IrConstructorCall.constructorClassFqName(): String? = try {
    symbol.owner.parentAsClass.kotlinFqName.asString()
} catch (_: Throwable) {
    null
}

private fun IrFunction.contextParameters(): List<IrValueParameter> =
    parameters.filter { it.kind == IrParameterKind.Context }
