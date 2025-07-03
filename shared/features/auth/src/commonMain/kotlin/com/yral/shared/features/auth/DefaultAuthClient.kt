package com.yral.shared.features.auth

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AuthSuccessfulEventData
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
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.CanistersWrapper
import com.yral.shared.uniffi.generated.FfiException
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Suppress("TooManyFunctions", "LongParameterList")
class DefaultAuthClient(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val preferences: Preferences,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val requiredUseCases: RequiredUseCases,
    private val oAuthUtils: OAuthUtils,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val scope: CoroutineScope,
) : AuthClient {
    private var currentState: String? = null

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
                skipFirebaseAuth = auth.currentUser != null,
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
                            resetCanister = true,
                            skipFirebaseAuth = false,
                        )
                    }.onFailure { throw YralAuthException("obtaining anonymous token failed - ${it.message}") }
            }.onFailure { throw YralAuthException("sign out of firebase failed - ${it.message}") }
    }

    private suspend fun handleToken(
        idToken: String,
        accessToken: String,
        refreshToken: String,
        resetCanister: Boolean = false,
        skipSetMetaData: Boolean = true,
        skipFirebaseAuth: Boolean = true,
    ) {
        crashlyticsManager.logMessage("parsing token")
        saveTokens(idToken, refreshToken, accessToken)
        val tokenClaim = oAuthUtils.parseOAuthToken(idToken)
        if (tokenClaim.isValid(Clock.System.now().epochSeconds)) {
            tokenClaim.delegatedIdentity?.let {
                if (resetCanister) {
                    resetCachedCanisterData()
                }
                handleExtractIdentityResponse(it, skipSetMetaData, skipFirebaseAuth)
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
        preferences.putString(PrefKeys.ID_TOKEN.name, idToken)
        if (refreshToken.isNotEmpty()) {
            preferences.putString(PrefKeys.REFRESH_TOKEN.name, refreshToken)
        }
        if (accessToken.isNotEmpty()) {
            preferences.putString(PrefKeys.ACCESS_TOKEN.name, refreshToken)
        }
    }

    override suspend fun logout() {
        analyticsManager.flush()
        listOf(
            PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name,
            PrefKeys.REFRESH_TOKEN.name,
            PrefKeys.ACCESS_TOKEN.name,
            PrefKeys.ID_TOKEN.name,
        ).forEach { key ->
            preferences.remove(key)
        }
        resetCachedCanisterData()
        sessionManager.updateState(SessionState.Initial)
        crashlyticsManager.setUserId("")
        requiredUseCases.signOutUseCase.invoke(Unit)
    }

    private suspend fun handleExtractIdentityResponse(
        data: ByteArray,
        skipSetMetaData: Boolean,
        skipFirebaseAuth: Boolean,
    ) {
        crashlyticsManager.logMessage("extracting identity")
        try {
            var cachedData = getCachedCanisterData()
            if (cachedData == null) {
                val canisterWrapper = authenticateWithNetwork(data, null)
                cacheCanisterData(data, canisterWrapper)
                cachedData =
                    CanisterData(
                        identity = data,
                        canisterId = canisterWrapper.getCanisterPrincipal(),
                        userPrincipalId = canisterWrapper.getUserPrincipal(),
                    )
            }
            if (!skipSetMetaData) {
                scope.launch {
                    updateYralSession(cachedData)
                }
            }
            sessionManager.updateCoinBalance(0)
            if (skipFirebaseAuth) {
                setSession(canisterData = cachedData)
            } else {
                authorizeFirebase(cachedData)
            }
        } catch (e: FfiException) {
            resetCachedCanisterData()
            crashlyticsManager.recordException(e)
            throw YralAuthException(e)
        }
    }

    private suspend fun updateYralSession(canisterData: CanisterData) {
        preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
            updateSessionAsRegistered(
                idToken = idToken,
                canisterId = canisterData.canisterId,
            )
        }
    }

    private suspend fun authorizeFirebase(canisterData: CanisterData) {
        crashlyticsManager.logMessage("authorizing firebase")
        requiredUseCases
            .signInAnonymouslyUseCase
            .invoke(Unit)
            .onSuccess {
                requiredUseCases
                    .getIdTokenUseCase
                    .invoke(GetIdTokenUseCase.DEFAULT)
                    .onSuccess { idToken ->
                        exchangePrincipalId(canisterData, idToken)
                    }.onFailure { throw YralAuthException("firebase idToken not found - ${it.message}") }
            }.onFailure { throw YralAuthException("firebase anonymous sign in failed - ${it.message}") }
    }

    private suspend fun exchangePrincipalId(
        canisterData: CanisterData,
        idToken: String,
    ) {
        crashlyticsManager.logMessage("exchanging principal id")
        requiredUseCases.exchangePrincipalIdUseCase
            .invoke(
                ExchangePrincipalIdUseCase.Params(
                    idToken = idToken,
                    userPrincipal = canisterData.userPrincipalId,
                ),
            ).onSuccess {
                signInWithToken(canisterData, it)
            }.onFailure { throw YralAuthException("exchanging principal failed - ${it.message}") }
    }

    private suspend fun signInWithToken(
        canisterData: CanisterData,
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
                        setSession(canisterData = canisterData)
                    }.onFailure { throw YralAuthException("sign in with token failed - ${it.message}") }
            }.onFailure { throw YralAuthException("sign out for auth token sign in failed - ${it.message}") }
    }

    private suspend fun updateBalanceAndProceed(canisterData: CanisterData) {
        crashlyticsManager.logMessage("getting balance")
        requiredUseCases.getBalanceUseCase
            .invoke(canisterData.userPrincipalId)
            .onSuccess { coinBalance ->
                crashlyticsManager.logMessage("updating user coins")
                requiredUseCases
                    .updateDocumentUseCase
                    .invoke(
                        parameter =
                            UpdateDocumentUseCase.Params(
                                collectionName = "users",
                                documentId = canisterData.userPrincipalId,
                                fieldAndValue = Pair("coins", coinBalance),
                            ),
                    ).onSuccess {
                        sessionManager.updateCoinBalance(coinBalance)
                    }.onFailure { throw YralAuthException("update coin balance failed ${it.message}") }
            }.onFailure { throw YralAuthException("get balance failed ${it.message}") }
    }

    private fun setSession(canisterData: CanisterData) {
        individualUserServiceFactory.initialize(
            principal = canisterData.canisterId,
            identityData = canisterData.identity,
        )
        sessionManager.updateState(
            SessionState.SignedIn(
                session =
                    Session(
                        identity = canisterData.identity,
                        canisterPrincipal = canisterData.canisterId,
                        userPrincipal = canisterData.userPrincipalId,
                    ),
            ),
        )
        scope.launch { updateBalanceAndProceed(canisterData) }
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

    override suspend fun signInWithSocial(provider: SocialProvider) {
        initiateOAuthFlow(provider)
    }

    private suspend fun initiateOAuthFlow(provider: SocialProvider) {
        sessionManager.getIdentity()?.let { identity ->
            val authUrl = authRepository.getOAuthUrl(provider, identity)
            currentState = authUrl.second
            oAuthUtils.openOAuth(
                authUrl = authUrl.first,
            ) { code, state ->
                scope.launch {
                    handleOAuthCallback(code, state)
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
        analyticsManager.flush()
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
                    resetCanister = true,
                    skipSetMetaData = false,
                    skipFirebaseAuth = false,
                )
                preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
            }.onFailure { throw YralAuthException("authenticate social sign in failed - ${it.message}") }
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

    private suspend fun getCachedCanisterData(): CanisterData? {
        val identity = preferences.getBytes(PrefKeys.IDENTITY.name)
        val canister = preferences.getString(PrefKeys.CANISTER_ID.name)
        val userPrincipal = preferences.getString(PrefKeys.USER_PRINCIPAL.name)
        return if (identity != null && canister != null && userPrincipal != null) {
            CanisterData(identity, canister, userPrincipal)
        } else {
            null
        }
    }

    private suspend fun cacheCanisterData(
        identity: ByteArray,
        canisterWrapper: CanistersWrapper,
    ) {
        preferences.putBytes(PrefKeys.IDENTITY.name, identity)
        preferences.putString(PrefKeys.CANISTER_ID.name, canisterWrapper.getCanisterPrincipal())
        preferences.putString(PrefKeys.USER_PRINCIPAL.name, canisterWrapper.getUserPrincipal())
    }

    private suspend fun resetCachedCanisterData() {
        preferences.remove(PrefKeys.IDENTITY.name)
        preferences.remove(PrefKeys.CANISTER_ID.name)
        preferences.remove(PrefKeys.USER_PRINCIPAL.name)
    }
}

data class CanisterData(
    val identity: ByteArray,
    val canisterId: String,
    val userPrincipalId: String,
)
