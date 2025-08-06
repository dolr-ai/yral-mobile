package com.yral.shared.features.auth

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.services.IndividualUserServiceFactory
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

@Suppress("LongParameterList")
class DefaultAuthClientFactory(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val preferences: Preferences,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val requiredUseCases: DefaultAuthClient.RequiredUseCases,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val oAuthUtils: OAuthUtils,
    private val oAuthUtilsHelper: OAuthUtilsHelper,
    private val authTelemetry: AuthTelemetry,
) : AuthClientFactory {
    override fun create(
        scope: CoroutineScope,
        onAuthError: (YralAuthException) -> Unit,
    ): AuthClient =
        DefaultAuthClient(
            sessionManager = sessionManager,
            analyticsManager = analyticsManager,
            crashlyticsManager = crashlyticsManager,
            preferences = preferences,
            auth = auth,
            authRepository = authRepository,
            requiredUseCases = requiredUseCases,
            individualUserServiceFactory = individualUserServiceFactory,
            oAuthUtils = oAuthUtils,
            oAuthUtilsHelper = oAuthUtilsHelper,
            authTelemetry = authTelemetry,
            scope =
                scope +
                    CoroutineExceptionHandler { _, throwable ->
                        when (throwable) {
                            is CancellationException -> {
                                throw throwable
                            }

                            is YralAuthException -> {
                                onAuthError(throwable)
                            }

                            else -> {
                                onAuthError(YralAuthException("Unknown - ${throwable.message}"))
                            }
                        }
                    },
        )
}
