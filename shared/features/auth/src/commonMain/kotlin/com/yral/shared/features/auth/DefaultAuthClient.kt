package com.yral.shared.features.auth

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AuthSuccessfulEventData
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ExchangePrincipalIdUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.utils.OAuthListener
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.game.domain.GetBalanceUseCase
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.firebaseAuth.usecase.SignInAnonymouslyUseCase
import com.yral.shared.firebaseAuth.usecase.SignInWithTokenUseCase
import com.yral.shared.firebaseAuth.usecase.SignOutUseCase
import com.yral.shared.firebaseStore.usecase.UpdateDocumentUseCase
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.CanistersWrapper
import com.yral.shared.uniffi.generated.FfiException
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DefaultAuthClient(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val preferences: Preferences,
    private val authRepository: AuthRepository,
    private val requiredUseCases: RequiredUseCases,
    private val oAuthUtils: OAuthUtils,
    appDispatchers: AppDispatchers,
) : AuthClient {
    private var currentState: String? = null
    private val scope = CoroutineScope(SupervisorJob() + appDispatchers.io)

    override suspend fun initialize() {
        sessionManager.updateState(SessionState.Loading)
        refreshAuthIfNeeded()
    }

    private suspend fun refreshAuthIfNeeded() {
        crashlyticsManager.logMessage("obtaining token from local storage")
        preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
            handleToken(
                idToken = idToken,
                accessToken = "",
                refreshToken = "",
            )
        } ?: obtainAnonymousIdentity()
    }

    private suspend fun obtainAnonymousIdentity() {
        crashlyticsManager.logMessage("signing out of firebase for obtaining anonymous token")
        requiredUseCases
            .signOutUseCase
            .invoke(Unit)
            .onSuccess {
                crashlyticsManager.logMessage("obtaining anonymous token")
                requiredUseCases.obtainAnonymousIdentityUseCase
                    .invoke(Unit)
                    .onSuccess { tokenResponse ->
                        handleToken(
                            idToken = tokenResponse.idToken,
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                            skipFirebaseAuth = false,
                        )
                    }.onFailure { throw YralException("obtaining anonymous token failed") }
            }.onFailure { throw YralException("sign out of firebase failed") }
    }

    private suspend fun handleToken(
        idToken: String,
        accessToken: String,
        refreshToken: String,
        shouldSetMetadata: Boolean = false,
        skipFirebaseAuth: Boolean = true,
    ) {
        crashlyticsManager.logMessage("parsing token")
        saveTokens(idToken, refreshToken, accessToken)
        val tokenClaim = oAuthUtils.parseOAuthToken(idToken)
        if (tokenClaim.isValid(Clock.System.now().epochSeconds)) {
            tokenClaim.delegatedIdentity?.let {
                handleExtractIdentityResponse(it, shouldSetMetadata, skipFirebaseAuth)
            }
        } else {
            refreshToken.takeIf { it.isNotEmpty() }?.let {
                val rTokenClaim = oAuthUtils.parseOAuthToken(it)
                if (rTokenClaim.isValid(Clock.System.now().epochSeconds)) {
                    refreshAccessToken()
                } else {
                    logout()
                }
            } ?: logout()
        }
    }

    private suspend fun saveTokens(
        idToken: String,
        refreshToken: String,
        accessToken: String,
    ) {
        crashlyticsManager.logMessage("setting token to local storage")
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
    }

    override suspend fun logout() {
        preferences.remove(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name)
        preferences.remove(PrefKeys.REFRESH_TOKEN.name)
        preferences.remove(PrefKeys.ACCESS_TOKEN.name)
        preferences.remove(PrefKeys.ID_TOKEN.name)
        preferences.remove(PrefKeys.IDENTITY.name)
        sessionManager.updateState(SessionState.Initial)
        crashlyticsManager.setUserId("")
        requiredUseCases.signOutUseCase.invoke(Unit)
    }

    private suspend fun handleExtractIdentityResponse(
        data: ByteArray,
        shouldSetMetaData: Boolean,
        skipFirebaseAuth: Boolean,
    ) {
        crashlyticsManager.logMessage("extracting identity")
        try {
            val canisterWrapper = authenticateWithNetwork(data, null)
            preferences.putBytes(PrefKeys.IDENTITY.name, data)
            if (shouldSetMetaData) {
                updateYralSession(canisterWrapper)
            }
            if (skipFirebaseAuth) {
                updateBalanceAndProceed(data, canisterWrapper)
            } else {
                authorizeFirebase(data, canisterWrapper)
            }
        } catch (e: FfiException) {
            crashlyticsManager.recordException(e)
            throw e
        }
    }

    private suspend fun updateYralSession(canisterWrapper: CanistersWrapper) {
        preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
            updateSessionAsRegistered(
                idToken = idToken,
                canisterId = canisterWrapper.getCanisterPrincipal(),
            )
        }
    }

    private suspend fun authorizeFirebase(
        data: ByteArray,
        canisterWrapper: CanistersWrapper,
    ) {
        crashlyticsManager.logMessage("authorizing firebase")
        requiredUseCases
            .signInAnonymouslyUseCase
            .invoke(Unit)
            .onSuccess {
                requiredUseCases
                    .getIdTokenUseCase
                    .invoke(GetIdTokenUseCase.DEFAULT)
                    .onSuccess { idToken ->
                        exchangePrincipalId(idToken, data, canisterWrapper)
                    }.onFailure { throw YralException("firebase idToken not found") }
            }.onFailure { throw YralException("firebase anonymous sign in failed") }
    }

    private suspend fun exchangePrincipalId(
        idToken: String,
        data: ByteArray,
        canisterWrapper: CanistersWrapper,
    ) {
        crashlyticsManager.logMessage("exchanging principal id")
        requiredUseCases.exchangePrincipalIdUseCase
            .invoke(
                ExchangePrincipalIdUseCase.Params(
                    idToken = idToken,
                    userPrincipal = canisterWrapper.getUserPrincipal(),
                ),
            ).onSuccess {
                signInWithToken(data, canisterWrapper, it)
            }.onFailure { throw YralException("exchanging principal failed") }
    }

    private suspend fun signInWithToken(
        data: ByteArray,
        canisterWrapper: CanistersWrapper,
        exchangeResult: ExchangePrincipalResponse,
    ) {
        crashlyticsManager.logMessage("signing with token")
        requiredUseCases
            .signOutUseCase
            .invoke(Unit)
            .onSuccess {
                requiredUseCases.signInWithTokenUseCase
                    .invoke(exchangeResult.token)
                    .onSuccess {
                        updateBalanceAndProceed(data, canisterWrapper)
                    }.onFailure { throw YralException("sign in with token failed") }
            }.onFailure { throw YralException("sign out for auth token sign in failed") }
    }

    private suspend fun updateBalanceAndProceed(
        data: ByteArray,
        canisterWrapper: CanistersWrapper,
    ) {
        crashlyticsManager.logMessage("getting balance")
        requiredUseCases.getBalanceUseCase
            .invoke(canisterWrapper.getUserPrincipal())
            .onSuccess { coinBalance ->
                crashlyticsManager.logMessage("updating user coins")
                requiredUseCases
                    .updateDocumentUseCase
                    .invoke(
                        parameter =
                            UpdateDocumentUseCase.Params(
                                collectionName = "users",
                                documentId = canisterWrapper.getUserPrincipal(),
                                fieldAndValue = Pair("coins", coinBalance),
                            ),
                    ).onSuccess {
                        setSession(data, canisterWrapper, coinBalance)
                    }.onFailure { throw YralException("update coin balance failed") }
            }.onFailure { throw YralException("get balance failed") }
    }

    private suspend fun setSession(
        data: ByteArray,
        canisterWrapper: CanistersWrapper,
        initialCoinBalance: Long,
    ) {
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
        sessionManager.updateCoinBalance(initialCoinBalance)
        analyticsManager.trackEvent(
            event = AuthSuccessfulEventData(),
        )
    }

    private suspend fun refreshAccessToken() {
        val refreshToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name)
        refreshToken?.let {
            requiredUseCases.refreshTokenUseCase
                .invoke(refreshToken)
                .onSuccess { tokenResponse ->
                    handleToken(
                        idToken = tokenResponse.idToken,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                    )
                }.onFailure { logout() }
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
                        handleOAuthCallback(code, state)
                    } catch (e: YralException) {
                        oAuthListener.yralException(e)
                    } catch (e: FfiException) {
                        oAuthListener.ffiException(e)
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
        sessionManager.updateState(SessionState.Loading)
        authenticate(code)
    }

    private suspend fun authenticate(code: String) {
        requiredUseCases.authenticateTokenUseCase
            .invoke(code)
            .onSuccess { tokenResponse ->
                handleToken(
                    idToken = tokenResponse.idToken,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    shouldSetMetadata = true,
                    skipFirebaseAuth = false,
                )
                preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
            }.onFailure { throw YralException("authenticate social sign in failed") }
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
        val signOutUseCase: SignOutUseCase,
        val signInAnonymouslyUseCase: SignInAnonymouslyUseCase,
        val signInWithTokenUseCase: SignInWithTokenUseCase,
        val exchangePrincipalIdUseCase: ExchangePrincipalIdUseCase,
        val getBalanceUseCase: GetBalanceUseCase,
        val updateDocumentUseCase: UpdateDocumentUseCase,
        val getIdTokenUseCase: GetIdTokenUseCase,
    )
}
