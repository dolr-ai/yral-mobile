package com.yral.shared.features.auth

import com.github.michaelbull.result.mapBoth
import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.Event
import com.yral.shared.analytics.main.FeatureEvents
import com.yral.shared.analytics.main.Features
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ExtractIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.SetAnonymousIdentityCookieUseCase
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.auth.utils.openOAuth
import com.yral.shared.features.auth.utils.parseAccessTokenForIdentity
import com.yral.shared.http.CookieType
import com.yral.shared.http.maxAgeOrExpires
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import io.ktor.http.Cookie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Suppress("TooGenericExceptionCaught", "LongParameterList")
class DefaultAuthClient(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val preferences: Preferences,
    private val platformResourcesFactory: PlatformResourcesFactory,
    private val authRepository: AuthRepository,
    private val setAnonymousIdentityCookieUseCase: SetAnonymousIdentityCookieUseCase,
    private val extractIdentityUseCase: ExtractIdentityUseCase,
    private val authenticateTokenUseCase: AuthenticateTokenUseCase,
    appDispatchers: AppDispatchers,
) : AuthClient {
    private val coroutineScope = CoroutineScope(appDispatchers.io)
    private var currentState: String? = null

    override suspend fun initialize() {
        checkSocialSignIn()
    }

    private suspend fun checkSocialSignIn() {
        if (preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) == true) {
            preferences.getBytes(PrefKeys.IDENTITY_DATA.name)?.let {
                handleExtractIdentityResponse(it)
            } ?: refreshAuthIfNeeded()
        } else {
            refreshAuthIfNeeded()
        }
    }

    private suspend fun refreshAuthIfNeeded() {
        val cookie = authRepository.getAnonymousIdentityCookie()
        cookie?.let {
            if ((it.maxAgeOrExpires(Clock.System.now().toEpochMilliseconds()) ?: 0) >
                Clock.System.now().toEpochMilliseconds()
            ) {
                val storedData = preferences.getBytes(PrefKeys.IDENTITY_DATA.name)
                storedData?.let { data -> handleExtractIdentityResponse(data) } ?: extractIdentity(
                    it,
                )
            } else {
                fetchAndSetAuthCookie()
            }
        } ?: fetchAndSetAuthCookie()
    }

    private suspend fun fetchAndSetAuthCookie() {
        setAnonymousIdentityCookieUseCase
            .invoke(Unit)
            .mapBoth(
                success = { refreshAuthIfNeeded() },
                failure = { error(it.localizedMessage ?: "") },
            )
        refreshAuthIfNeeded()
    }

    private suspend fun extractIdentity(cookie: Cookie) {
        extractIdentityUseCase
            .invoke(cookie)
            .mapBoth(
                success = {
                    if (it.isNotEmpty()) {
                        handleExtractIdentityResponse(it)
                    }
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

    override fun handleOAuthCallback(
        code: String,
        state: String,
    ) {
        if (state != currentState) {
            throw SecurityException("Invalid state parameter - possible CSRF attack")
        }
        authenticate(code)
    }

    private fun authenticate(code: String) {
        coroutineScope.launch {
            try {
                authenticateTokenUseCase
                    .invoke(code)
                    .mapBoth(
                        success = { tokenResponse ->
                            val identity = parseAccessTokenForIdentity(tokenResponse.accessToken)
                            preferences.putString(
                                PrefKeys.REFRESH_TOKEN.name,
                                tokenResponse.refreshToken,
                            )
                            preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
                            preferences.remove(CookieType.USER_IDENTITY.value)
                            handleExtractIdentityResponse(identity)
                        },
                        failure = { error(it.localizedMessage ?: "") },
                    )
            } catch (e: Exception) {
                crashlyticsManager.recordException(e)
            }
        }
    }
}
