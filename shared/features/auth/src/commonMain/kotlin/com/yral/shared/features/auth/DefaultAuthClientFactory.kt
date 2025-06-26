package com.yral.shared.features.auth

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

class DefaultAuthClientFactory(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val preferences: Preferences,
    private val authRepository: AuthRepository,
    private val requiredUseCases: DefaultAuthClient.RequiredUseCases,
    private val oAuthUtils: OAuthUtils,
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
            authRepository = authRepository,
            requiredUseCases = requiredUseCases,
            oAuthUtils = oAuthUtils,
            scope =
                scope +
                    CoroutineExceptionHandler { _, throwable ->
                        if (throwable is YralAuthException) {
                            onAuthError(throwable)
                        } else {
                            onAuthError(YralAuthException("Unknown - ${throwable.message}"))
                        }
                    },
        )
}
