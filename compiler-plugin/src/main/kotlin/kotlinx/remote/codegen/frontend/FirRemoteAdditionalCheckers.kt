/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.codegen.frontend

import kotlinx.remote.codegen.common.RemoteClassId
import kotlinx.remote.codegen.frontend.diagnostics.FirRemoteDiagnostics.GENERIC_REMOTE_FUNCTION
import kotlinx.remote.codegen.frontend.diagnostics.FirRemoteDiagnostics.WRONG_REMOTE_FUNCTION_CONTEXT
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isSubtypeOf

class FirRemoteAdditionalCheckers(
    session: FirSession,
) : FirAdditionalCheckersExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirRemotePredicates.remote)
    }

    override val declarationCheckers: DeclarationCheckers = FirRemoteDeclarationCheckers()
}

class FirRemoteDeclarationCheckers : DeclarationCheckers() {
    override val functionCheckers: Set<FirFunctionChecker> = setOf(
        FirRemoteFunctionContextChecker(),
        FirGenericRemoteFunctionChecker(),
    )
}

class FirRemoteFunctionContextChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (!context.session.predicateBasedProvider.matches(FirRemotePredicates.remote, declaration)) {
            return
        }
        val remoteContextType = RemoteClassId.remoteContext.constructClassLikeType()
        val remoteContextParameters = declaration.contextParameters.filter {
            it.symbol.resolvedReturnTypeRef.coneType.isSubtypeOf(
                remoteContextType,
                context.session
            )
        }
        if (remoteContextParameters.size != 1) {
            reporter.reportOn(
                source = declaration.source,
                factory = WRONG_REMOTE_FUNCTION_CONTEXT,
                a = remoteContextType,
            )
        }
    }
}

class FirGenericRemoteFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (!context.session.predicateBasedProvider.matches(FirRemotePredicates.remote, declaration)) {
            return
        }
        if (declaration.typeParameters.isNotEmpty()) {
            reporter.reportOn(
                source = declaration.source,
                factory = GENERIC_REMOTE_FUNCTION,
            )
        }
    }
}

