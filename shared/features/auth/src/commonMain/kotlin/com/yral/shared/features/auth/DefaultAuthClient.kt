package com.yral.shared.features.auth

import com.github.michaelbull.result.mapBoth
import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.Event
import com.yral.shared.analytics.main.FeatureEvents
import com.yral.shared.analytics.main.Features
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.auth.utils.openOAuth
import com.yral.shared.features.auth.utils.parseAccessTokenForIdentity
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.authenticateWithNetwork

class DefaultAuthClient(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val preferences: Preferences,
    private val platformResourcesFactory: PlatformResourcesFactory,
    private val authRepository: AuthRepository,
    private val authenticateTokenUseCase: AuthenticateTokenUseCase,
    private val obtainAnonymousIdentityUseCase: ObtainAnonymousIdentityUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
) : AuthClient {
    private var currentState: String? = null

    override suspend fun initialize() {
        refreshAuthIfNeeded()
    }

    private suspend fun refreshAuthIfNeeded() {
        preferences.getBytes(PrefKeys.IDENTITY_DATA.name)?.let {
            handleExtractIdentityResponse(it)
        } ?: obtainAnonymousIdentity()
    }

    private suspend fun obtainAnonymousIdentity() {
        obtainAnonymousIdentityUseCase
            .invoke(Unit)
            .mapBoth(
                success = { tokenResponse ->
                    val identity = parseAccessTokenForIdentity(tokenResponse.accessToken)
                    handleExtractIdentityResponse(identity)
                    preferences.putString(
                        PrefKeys.REFRESH_TOKEN.name,
                        tokenResponse.refreshToken,
                    )
                },
                failure = { error(it.localizedMessage ?: "") },
            )
    }

    private suspend fun handleExtractIdentityResponse(data: ByteArray) {
        val canisterWrapper = authenticateWithNetwork(data, null)
        sessionManager.updateState(
            SessionState.SignedIn(
                session =
                    Session(
                        identity = data,
                        canisterPrincipal = canisterWrapper.getCanisterPrincipal(),
                        userPrincipal = canisterWrapper.getUserPrincipal(),
                    ),
            ),
        )
        preferences.putBytes(PrefKeys.IDENTITY_DATA.name, data)
        analyticsManager.trackEvent(
            event =
                Event(
                    featureName = Features.AUTH.name.lowercase(),
                    name = FeatureEvents.AUTH_SUCCESSFUL.name.lowercase(),
                ),
        )
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun refreshAccessToken() {
        val refreshToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name)
        refreshToken?.let {
            refreshTokenUseCase
                .invoke(refreshToken)
                .mapBoth(
                    success = { tokenResponse ->
                        val identity = parseAccessTokenForIdentity(tokenResponse.accessToken)
                        handleExtractIdentityResponse(identity)
                        preferences.putString(
                            PrefKeys.REFRESH_TOKEN.name,
                            tokenResponse.refreshToken,
                        )
                    },
                    failure = { error(it.localizedMessage ?: "") },
                )
        } ?: obtainAnonymousIdentity()
    }

    override suspend fun signInWithSocial(provider: SocialProvider) {
        initiateOAuthFlow(provider)
    }

    private suspend fun initiateOAuthFlow(provider: SocialProvider) {
        sessionManager.getIdentity()?.let { identity ->
            val authUrl = authRepository.getOAuthUrl(provider, identity)
            currentState = authUrl.second
            openOAuth(
                platformResourcesFactory = platformResourcesFactory,
                authUrl = authUrl.first,
            )
        }
    }

    override suspend fun handleOAuthCallback(
        code: String,
        state: String,
    ) {
        if (state != currentState) {
            throw SecurityException("Invalid state parameter - possible CSRF attack")
        }
        authenticate(code)
    }

    private suspend fun authenticate(code: String) {
        authenticateTokenUseCase
            .invoke(code)
            .mapBoth(
                success = { tokenResponse ->
                    val identity = parseAccessTokenForIdentity(tokenResponse.accessToken)
                    handleExtractIdentityResponse(identity)
                    preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
                    preferences.putString(
                        PrefKeys.REFRESH_TOKEN.name,
                        tokenResponse.refreshToken,
                    )
                },
                failure = { error(it.localizedMessage ?: "") },
            )
    }
}
