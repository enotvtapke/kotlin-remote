/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("StructuralWrap")

package kotlinx.remote.codegen.frontend.diagnostics

import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement

// ###########################################################################
// ###                     BIG WARNING, LISTEN CLOSELY!                    ###
// # Do NOT use `PsiElement` for `error0` or any other function              #
// # Instead use KtElement, otherwise problems in IDE and in tests may arise #
// ###########################################################################

object FirRemoteDiagnostics {
    val WRONG_REMOTE_FUNCTION_CONTEXT by error1<KtElement, ConeKotlinType>()
    val GENERIC_REMOTE_FUNCTION by error0<KtElement>()
}
