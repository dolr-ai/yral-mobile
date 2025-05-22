package com.yral.shared.features.auth

import com.github.michaelbull.result.mapBoth
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AuthSuccessfulEventData
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.utils.OAuthListener
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DefaultAuthClient(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val preferences: Preferences,
    private val authRepository: AuthRepository,
    private val requiredUseCases: RequiredUseCases,
    private val oAuthUtils: OAuthUtils,
    appDispatchers: AppDispatchers,
) : AuthClient {
    private var currentState: String? = null
    private val scope = CoroutineScope(appDispatchers.io)

    override suspend fun initialize() {
        refreshAuthIfNeeded()
    }

    private suspend fun refreshAuthIfNeeded() {
        preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
            handleToken(
                idToken = idToken,
                accessToken = "",
                refreshToken = "",
                shouldRefreshToken = true,
            )
        } ?: obtainAnonymousIdentity()
    }

    private suspend fun obtainAnonymousIdentity() {
        requiredUseCases.obtainAnonymousIdentityUseCase
            .invoke(Unit)
            .mapBoth(
                success = { tokenResponse ->
                    handleToken(
                        idToken = tokenResponse.idToken,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        shouldRefreshToken = true,
                    )
                },
                failure = { error(it.localizedMessage ?: "") },
            )
    }

    private suspend fun handleToken(
        idToken: String,
        accessToken: String,
        refreshToken: String,
        shouldRefreshToken: Boolean,
        shouldSetMetadata: Boolean = false,
    ) {
        preferences.putString(
            PrefKeys.ID_TOKEN.name,
            idToken,
        )
        if (refreshToken.isNotEmpty()) {
            preferences.putString(
                PrefKeys.REFRESH_TOKEN.name,
                refreshToken,
            )
        }
        if (accessToken.isNotEmpty()) {
            preferences.putString(
                PrefKeys.ACCESS_TOKEN.name,
                refreshToken,
            )
        }
        val tokenClaim = oAuthUtils.parseOAuthToken(idToken)
        if (tokenClaim.isValid(Clock.System.now().epochSeconds)) {
            tokenClaim.delegatedIdentity?.let {
                handleExtractIdentityResponse(it, shouldSetMetadata)
            }
        } else if (shouldRefreshToken) {
            val rToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name)
            rToken?.let {
                val rTokenClaim = oAuthUtils.parseOAuthToken(it)
                if (rTokenClaim.isValid(Clock.System.now().epochSeconds)) {
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
        preferences.remove(PrefKeys.ID_TOKEN.name)
        preferences.remove(PrefKeys.IDENTITY.name)
        sessionManager.updateState(SessionState.Initial)
    }

    private suspend fun handleExtractIdentityResponse(
        data: ByteArray,
        shouldSetMetaData: Boolean,
    ) {
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
        if (shouldSetMetaData) {
            preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
                sessionManager.getCanisterPrincipal()?.let { canisterId ->
                    updateSessionAsRegistered(
                        idToken = idToken,
                        canisterId = canisterId,
                    )
                }
            }
        }
        analyticsManager.trackEvent(
            event = AuthSuccessfulEventData(),
        )
    }

    private suspend fun refreshAccessToken() {
        val refreshToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name)
        refreshToken?.let {
            requiredUseCases.refreshTokenUseCase
                .invoke(refreshToken)
                .mapBoth(
                    success = { tokenResponse ->
                        handleToken(
                            idToken = tokenResponse.idToken,
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                            shouldRefreshToken = true,
                        )
                    },
                    failure = { error(it.localizedMessage ?: "") },
                )
        } ?: obtainAnonymousIdentity()
    }

    override suspend fun signInWithSocial(
        provider: SocialProvider,
        oAuthListener: OAuthListener,
    ) {
        initiateOAuthFlow(provider, oAuthListener)
    }

    private suspend fun initiateOAuthFlow(
        provider: SocialProvider,
        oAuthListener: OAuthListener,
    ) {
        sessionManager.getIdentity()?.let { identity ->
            val authUrl = authRepository.getOAuthUrl(provider, identity)
            currentState = authUrl.second
            oAuthUtils.openOAuth(
                authUrl = authUrl.first,
            ) { code, state ->
                scope.launch {
                    try {
                        oAuthListener.setLoading(true)
                        handleOAuthCallback(code, state)
                        oAuthListener.setLoading(false)
                    } catch (e: YralException) {
                        oAuthListener.exception(e)
                    }
                }
            }
        }
    }

    private suspend fun handleOAuthCallback(
        code: String,
        state: String,
    ) {
        if (state != currentState) {
            throw SecurityException("Invalid state parameter - possible CSRF attack")
        }
        authenticate(code)
    }

    private suspend fun authenticate(code: String) {
        requiredUseCases.authenticateTokenUseCase
            .invoke(code)
            .mapBoth(
                success = { tokenResponse ->
                    handleToken(
                        idToken = tokenResponse.idToken,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        shouldRefreshToken = true,
                        shouldSetMetadata = true,
                    )
                    preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
                },
                failure = { YralException(it.localizedMessage ?: "") },
            )
    }

    private suspend fun updateSessionAsRegistered(
        idToken: String,
        canisterId: String,
    ) {
        requiredUseCases.updateSessionAsRegisteredUseCase.invoke(
            parameter =
                UpdateSessionAsRegisteredUseCase.Params(
                    idToken = idToken,
                    canisterId = canisterId,
                ),
        )
    }

    data class RequiredUseCases(
        val authenticateTokenUseCase: AuthenticateTokenUseCase,
        val obtainAnonymousIdentityUseCase: ObtainAnonymousIdentityUseCase,
        val refreshTokenUseCase: RefreshTokenUseCase,
        val updateSessionAsRegisteredUseCase: UpdateSessionAsRegisteredUseCase,
    )
}
