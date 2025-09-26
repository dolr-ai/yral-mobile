package com.yral.shared.features.auth

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.session.DELAY_FOR_SESSION_PROPERTIES
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ExchangePrincipalIdUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.utils.OAuthResult
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.game.domain.GetBalanceUseCase
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.firebaseAuth.usecase.SignInAnonymouslyUseCase
import com.yral.shared.firebaseAuth.usecase.SignInWithTokenUseCase
import com.yral.shared.firebaseAuth.usecase.SignOutUseCase
import com.yral.shared.firebaseStore.usecase.UpdateDocumentUseCase
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.YralFfiException
import com.yral.shared.rust.service.utils.authenticateWithNetwork
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
    private val oAuthUtilsHelper: OAuthUtilsHelper,
    private val scope: CoroutineScope,
    private val authTelemetry: AuthTelemetry,
    private val initRustFactories: (identity: ByteArray) -> Unit,
) : AuthClient {
    private var currentState: String? = null

    override suspend fun initialize() {
        sessionManager.updateState(SessionState.Loading)
        refreshAuthIfNeeded()
    }

    private suspend fun refreshAuthIfNeeded() {
        preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
            handleToken(
                idToken = idToken,
                accessToken = "",
                refreshToken = "",
            )
        } ?: obtainAnonymousIdentity()
    }

    private suspend fun obtainAnonymousIdentity() {
        requiredUseCases.obtainAnonymousIdentityUseCase
            .invoke(Unit)
            .onSuccess { tokenResponse ->
                handleToken(
                    idToken = tokenResponse.idToken,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    resetCanister = true,
                )
                sessionManager.updateSocialSignInStatus(false)
            }.onFailure { throw YralAuthException("obtaining anonymous token failed - ${it.message}") }
    }

    private suspend fun handleToken(
        idToken: String,
        accessToken: String,
        refreshToken: String,
        resetCanister: Boolean = false,
    ) {
        saveTokens(idToken, refreshToken, accessToken)
        val tokenClaim = oAuthUtilsHelper.parseOAuthToken(idToken)
        if (tokenClaim.isValid(Clock.System.now().epochSeconds)) {
            tokenClaim.delegatedIdentity?.let {
                if (resetCanister) {
                    resetCachedCanisterData()
                }
                handleExtractIdentityResponse(it)
            }
            tokenClaim.email?.let {
                sessionManager.updateLoggedInUserEmail(it)
            }
        } else {
            refreshToken.takeIf { it.isNotEmpty() }?.let {
                val rTokenClaim = oAuthUtilsHelper.parseOAuthToken(it)
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
        preferences.putString(PrefKeys.ID_TOKEN.name, idToken)
        if (refreshToken.isNotEmpty()) {
            preferences.putString(PrefKeys.REFRESH_TOKEN.name, refreshToken)
        }
        if (accessToken.isNotEmpty()) {
            preferences.putString(PrefKeys.ACCESS_TOKEN.name, refreshToken)
        }
    }

    override suspend fun logout() {
        // clear preferences
        listOf(
            PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name,
            PrefKeys.REFRESH_TOKEN.name,
            PrefKeys.ACCESS_TOKEN.name,
            PrefKeys.ID_TOKEN.name,
            PrefKeys.HOW_TO_PLAY_SHOWN.name,
            // PrefKeys.SMILEY_GAME_NUDGE_SHOWN.name,
        ).forEach { key ->
            preferences.remove(key)
        }
        requiredUseCases.deregisterNotificationTokenUseCase(
            DeregisterNotificationTokenUseCase.Parameter(
                token = Firebase.messaging.getToken(),
            ),
        )
        // clear cached canister data after parsing token
        resetCachedCanisterData()
        // reset analytics manage: flush events and reset user properties
        analyticsManager.reset()
        // reset session manager for properties
        sessionManager.resetSessionProperties()
        // reset crashlytics
        crashlyticsManager.setUserId("")
        // logout of firebase
        requiredUseCases.signOutUseCase.invoke(Unit)
        // set session state to initial for re-login
        sessionManager.updateState(SessionState.Initial)
    }

    private suspend fun handleExtractIdentityResponse(data: ByteArray) {
        try {
            var cachedSession = getCachedSession()
            if (cachedSession == null) {
                val canisterWrapper = authenticateWithNetwork(data)
                cacheSession(data, canisterWrapper)
                cachedSession =
                    Session(
                        identity = data,
                        canisterId = canisterWrapper.canisterId,
                        userPrincipal = canisterWrapper.userPrincipalId,
                        profilePic = canisterWrapper.profilePic,
                        isCreatedFromServiceCanister = canisterWrapper.isCreatedFromServiceCanister,
                    )
            }
            cachedSession.userPrincipal?.let { crashlyticsManager.setUserId(it) }
            sessionManager.updateCoinBalance(0)
            setSession(session = cachedSession)
            fetchBalance(session = cachedSession)
            if (auth.currentUser?.uid == cachedSession.userPrincipal) {
                sessionManager.updateFirebaseLoginState(true)
                postFirebaseLogin(cachedSession)
            } else {
                sessionManager.updateFirebaseLoginState(false)
            }
        } catch (e: YralFfiException) {
            resetCachedCanisterData()
            crashlyticsManager.recordException(e)
            throw YralAuthException(e)
        }
    }

    private suspend fun updateYralSession(session: Session) {
        preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
            session.canisterId?.let { canisterId ->
                session.userPrincipal?.let { userPrincipal ->
                    updateSessionAsRegistered(
                        idToken = idToken,
                        canisterId = canisterId,
                        userPrincipal = userPrincipal,
                    )
                }
            }
        }
    }

    override suspend fun authorizeFirebase(session: Session) {
        requiredUseCases
            .signOutUseCase
            .invoke(Unit)
            .onSuccess {
                requiredUseCases
                    .signInAnonymouslyUseCase
                    .invoke(Unit)
                    .onSuccess { getIdTokenAndProceed(session) }
                    .onFailure { throw YralFBAuthException("firebase anonymous sign in failed - ${it.message}") }
            }.onFailure { throw YralFBAuthException("sign out for anonymous sign in failed - ${it.message}") }
    }

    private suspend fun getIdTokenAndProceed(session: Session) {
        requiredUseCases
            .getIdTokenUseCase
            .invoke(GetIdTokenUseCase.DEFAULT)
            .onSuccess { idToken -> exchangePrincipalId(session, idToken) }
            .onFailure { throw YralFBAuthException("firebase idToken not found - ${it.message}") }
    }

    private suspend fun exchangePrincipalId(
        session: Session,
        idToken: String,
    ) {
        session.userPrincipal?.let { userPrincipal ->
            requiredUseCases.exchangePrincipalIdUseCase
                .invoke(
                    ExchangePrincipalIdUseCase.Params(
                        idToken = idToken,
                        userPrincipal = userPrincipal,
                    ),
                ).onSuccess { signInWithToken(session, it) }
                .onFailure { throw YralFBAuthException("exchanging principal failed - ${it.message}") }
        } ?: throw YralFBAuthException("exchanging principal failed - user principal not found")
    }

    private suspend fun signInWithToken(
        session: Session,
        exchangeResult: ExchangePrincipalResponse,
    ) {
        requiredUseCases
            .signOutUseCase
            .invoke(Unit)
            .onSuccess {
                requiredUseCases.signInWithTokenUseCase
                    .invoke(exchangeResult.token)
                    .onSuccess {
                        sessionManager.updateFirebaseLoginState(true)
                        postFirebaseLogin(session)
                    }.onFailure { throw YralFBAuthException("sign in with token failed - ${it.message}") }
            }.onFailure { throw YralFBAuthException("sign out for auth token sign in failed - ${it.message}") }
    }

    override suspend fun fetchBalance(session: Session) {
        session.userPrincipal?.let { userPrincipal ->
            requiredUseCases.getBalanceUseCase
                .invoke(userPrincipal)
                .onSuccess { coinBalance -> sessionManager.updateCoinBalance(coinBalance) }
        }
    }

    @Suppress("ThrowsCount")
    private suspend fun updateBalanceAndProceed(session: Session) {
        session.userPrincipal?.let { userPrincipal ->
            requiredUseCases.getBalanceUseCase
                .invoke(userPrincipal)
                .onSuccess { coinBalance ->
                    // Update last known coin balance irrespective of firebase login
                    sessionManager.updateCoinBalance(coinBalance)
                    requiredUseCases
                        .updateDocumentUseCase
                        .invoke(
                            parameter =
                                UpdateDocumentUseCase.Params(
                                    collectionName = "users",
                                    documentId = userPrincipal,
                                    fieldAndValue = Pair("coins", coinBalance),
                                ),
                        ).onFailure { throw YralFBAuthException("update coin balance failed ${it.message}") }
                }.onFailure { throw YralAuthException("get balance failed ${it.message}") }
        } ?: throw YralAuthException("get balance failed - user principal not found")
    }

    private fun setSession(session: Session) {
        session.identity?.let { identity -> initRustFactories(identity) }
        sessionManager.updateState(SessionState.SignedIn(session = session))
    }

    private fun postFirebaseLogin(session: Session) {
        scope.launch { updateBalanceAndProceed(session) }
        scope.launch {
            val result =
                requiredUseCases.registerNotificationTokenUseCase(
                    RegisterNotificationTokenUseCase.Parameter(
                        token = Firebase.messaging.getToken(),
                    ),
                )
            Logger.d(DefaultAuthClient::class.simpleName!!) { "Notification token registered: $result" }
        }
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
        context: Any,
        provider: SocialProvider,
    ) {
        initiateOAuthFlow(context, provider)
    }

    private suspend fun initiateOAuthFlow(
        context: Any,
        provider: SocialProvider,
    ) {
        sessionManager.identity?.let { identity ->
            val authUrl = authRepository.getOAuthUrl(provider, identity)
            currentState = authUrl.second
            oAuthUtils.openOAuth(
                authUrl = authUrl.first,
                context = context,
            ) { result -> scope.launch { handleOAuthCallback(result) } }
        }
    }

    private suspend fun handleOAuthCallback(result: OAuthResult) {
        lateinit var error: String
        when (result) {
            is OAuthResult.Success -> {
                if (result.state != currentState) {
                    authTelemetry.authFailed()
                    throw SecurityException("Invalid state parameter - possible CSRF attack")
                }
                val currentUser = sessionManager.userPrincipal
                // reset analytics manage: flush events and reset user properties
                analyticsManager.reset()
                sessionManager.updateState(SessionState.Loading)
                authenticate(result.code, currentUser)
                return
            }
            is OAuthResult.Error -> {
                error = result.errorDescription?.let { "${result.error}: $it" } ?: result.error
            }
            is OAuthResult.Cancelled -> {
                error = "OAuth authentication was cancelled by user"
            }
            is OAuthResult.TimedOut -> {
                error = "OAuth authentication timed out - please try again"
            }
        }
        authTelemetry.authFailed()
        throw YralAuthException("OAuth authentication failed - $error")
    }

    private suspend fun authenticate(
        code: String,
        currentUserPrincipal: String?,
    ) {
        requiredUseCases.authenticateTokenUseCase
            .invoke(code)
            .onSuccess { tokenResponse ->
                handleToken(
                    idToken = tokenResponse.idToken,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    resetCanister = true,
                )
                preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
                sessionManager.updateSocialSignInStatus(true)
                scope.launch { getCachedSession()?.let { updateYralSession(it) } }
                scope.launch {
                    // Minor delay for super properties to be set
                    delay(DELAY_FOR_SESSION_PROPERTIES)
                    authTelemetry.onAuthSuccess(
                        isNewUser = currentUserPrincipal == sessionManager.userPrincipal,
                    )
                }
            }.onFailure {
                authTelemetry.authFailed()
                throw YralAuthException("authenticate social sign in failed - ${it.message}")
            }
    }

    private suspend fun updateSessionAsRegistered(
        idToken: String,
        canisterId: String,
        userPrincipal: String,
    ) {
        requiredUseCases.updateSessionAsRegisteredUseCase.invoke(
            parameter =
                UpdateSessionAsRegisteredUseCase.Params(
                    idToken = idToken,
                    canisterId = canisterId,
                    userPrincipal = userPrincipal,
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
        val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase,
        val deregisterNotificationTokenUseCase: DeregisterNotificationTokenUseCase,
    )

    private suspend fun getCachedSession(): Session? {
        val identity = preferences.getBytes(PrefKeys.IDENTITY.name)
        val canisterId = preferences.getString(PrefKeys.CANISTER_ID.name)
        val userPrincipal = preferences.getString(PrefKeys.USER_PRINCIPAL.name)
        val profilePic = preferences.getString(PrefKeys.PROFILE_PIC.name)
        val isCreatedFromServiceCanister = preferences.getBoolean(PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name)
        return listOf(identity, canisterId, userPrincipal, profilePic)
            .all { it != null }
            .let { allPresent ->
                if (allPresent) {
                    Session(
                        identity = identity!!,
                        canisterId = canisterId!!,
                        userPrincipal = userPrincipal!!,
                        profilePic = profilePic!!,
                        // default false for backward compatibility
                        isCreatedFromServiceCanister = isCreatedFromServiceCanister ?: false,
                    )
                } else {
                    null
                }
            }
    }

    private suspend fun cacheSession(
        identity: ByteArray,
        canisterWrapper: CanisterData,
    ) {
        with(canisterWrapper) {
            preferences.putBytes(PrefKeys.IDENTITY.name, identity)
            preferences.putString(PrefKeys.CANISTER_ID.name, canisterId)
            preferences.putString(PrefKeys.USER_PRINCIPAL.name, userPrincipalId)
            preferences.putString(PrefKeys.PROFILE_PIC.name, profilePic)
            preferences.putBoolean(PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name, isCreatedFromServiceCanister)
        }
    }

    private suspend fun resetCachedCanisterData() {
        preferences.remove(PrefKeys.IDENTITY.name)
        preferences.remove(PrefKeys.CANISTER_ID.name)
        preferences.remove(PrefKeys.USER_PRINCIPAL.name)
        preferences.remove(PrefKeys.PROFILE_PIC.name)
        preferences.remove(PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name)
    }
}
