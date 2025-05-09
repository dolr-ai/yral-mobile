package com.yral.shared.features.auth

import com.github.michaelbull.result.mapBoth
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AuthSuccessfulEventData
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
import com.yral.shared.features.auth.utils.parseOAuthToken
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import kotlinx.datetime.Clock

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
        preferences.getString(PrefKeys.ACCESS_TOKEN.name)?.let { accessToken ->
            handleToken(
                token = accessToken,
                refreshToken = "",
                shouldRefreshToken = true,
            )
        } ?: obtainAnonymousIdentity()
    }

    private suspend fun obtainAnonymousIdentity() {
        obtainAnonymousIdentityUseCase
            .invoke(Unit)
            .mapBoth(
                success = { tokenResponse ->
                    handleToken(
                        token = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        shouldRefreshToken = true,
                    )
                },
                failure = { error(it.localizedMessage ?: "") },
            )
    }

    private suspend fun handleToken(
        token: String,
        refreshToken: String,
        shouldRefreshToken: Boolean,
    ) {
        preferences.putString(
            PrefKeys.ACCESS_TOKEN.name,
            token,
        )
        if (refreshToken.isNotEmpty()) {
            preferences.putString(
                PrefKeys.REFRESH_TOKEN.name,
                refreshToken,
            )
        }
        val tokenClaim = parseOAuthToken(token)
        if (tokenClaim.isExpired(Clock.System.now().epochSeconds)) {
            tokenClaim.delegatedIdentity?.let {
                handleExtractIdentityResponse(it)
            }
        } else if (shouldRefreshToken) {
            val rToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name)
            rToken?.let {
                val rTokenClaim = parseOAuthToken(it)
                if (rTokenClaim.isExpired(Clock.System.now().epochSeconds)) {
                    refreshAccessToken()
                } else {
                    logout()
                }
            } ?: logout()
        }
    }

    override suspend fun logout() {
        preferences.remove(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name)
        preferences.remove(PrefKeys.REFRESH_TOKEN.name)
        preferences.remove(PrefKeys.ACCESS_TOKEN.name)
        preferences.remove(PrefKeys.IDENTITY.name)
        sessionManager.updateState(SessionState.Initial)
    }

    private suspend fun handleExtractIdentityResponse(data: ByteArray) {
        val canisterWrapper = authenticateWithNetwork(data, null)
        preferences.putBytes(PrefKeys.IDENTITY.name, data)
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
        analyticsManager.trackEvent(
            event = AuthSuccessfulEventData(),
        )
    }

    private suspend fun refreshAccessToken() {
        val refreshToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name)
        refreshToken?.let {
            refreshTokenUseCase
                .invoke(refreshToken)
                .mapBoth(
                    success = { tokenResponse ->
                        handleToken(
                            token = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                            shouldRefreshToken = true,
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
                    handleToken(
                        token = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        shouldRefreshToken = true,
                    )
                    preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
                },
                failure = { error(it.localizedMessage ?: "") },
            )
    }
}
