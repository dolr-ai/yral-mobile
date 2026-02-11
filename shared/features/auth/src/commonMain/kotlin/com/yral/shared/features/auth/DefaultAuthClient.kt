package com.yral.shared.features.auth

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.AuthSessionCause
import com.yral.shared.analytics.events.AuthSessionFlow
import com.yral.shared.analytics.events.AuthSessionInitiator
import com.yral.shared.analytics.events.AuthSessionState
import com.yral.shared.analytics.events.OtpValidationStatus
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.DELAY_FOR_SESSION_PROPERTIES
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.features.auth.domain.models.PhoneAuthLoginResponse
import com.yral.shared.features.auth.domain.models.PhoneAuthVerifyResponse
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ExchangePrincipalIdUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.PhoneAuthLoginUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.domain.useCases.VerifyPhoneAuthUseCase
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
import com.yral.shared.rust.service.utils.getSessionFromIdentity
import dev.gitlive.firebase.auth.FirebaseAuth
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("TooManyFunctions", "LongParameterList", "LargeClass")
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
    private var currentProvider: SocialProvider? = null
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun initialize() {
        sessionManager.updateState(SessionState.Loading)
        refreshAuthIfNeeded()
    }

    private suspend fun refreshAuthIfNeeded() {
        val lastActivePrincipal = preferences.getString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name)
        val mainPrincipal = preferences.getString(PrefKeys.MAIN_PRINCIPAL.name)
        // If last active was a bot (or non-main) and we have it cached, restore it immediately
        // and skip main token handling.
        if (lastActivePrincipal != null && lastActivePrincipal != mainPrincipal) {
            getCachedSession()?.takeIf { it.userPrincipal == lastActivePrincipal }?.let { cached ->
                setSession(cached)
                // Refresh tokens to keep bot delegations alive without switching away from bot session
                refreshTokensSilently()
                return
            }
        }
        preferences.getString(PrefKeys.ID_TOKEN.name)?.let { idToken ->
            val shouldPersistTokenState =
                lastActivePrincipal == null || lastActivePrincipal == mainPrincipal
            handleToken(
                idToken = idToken,
                accessToken = "",
                refreshToken = "",
                persistTokenState = shouldPersistTokenState,
                persistBotIdentities = false,
            )
        } ?: obtainAnonymousIdentity()
    }

    private suspend fun refreshTokensSilently() {
        val refreshToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name) ?: return
        requiredUseCases.refreshTokenUseCase
            .invoke(refreshToken)
            .onSuccess { tokenResponse ->
                saveTokens(
                    idToken = tokenResponse.idToken,
                    refreshToken = tokenResponse.refreshToken,
                    accessToken = tokenResponse.accessToken,
                    persistBotIdentities = false,
                )
            }.onFailure {
                Logger.e("DefaultAuthClient") { "Silent token refresh failed: ${it.message}" }
            }
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
            }.onFailure {
                authTelemetry.anonymousAuthFailed(it.message)
                throw YralAuthException("obtaining anonymous token failed - ${it.message}")
            }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun handleToken(
        idToken: String,
        accessToken: String,
        refreshToken: String,
        resetCanister: Boolean = false,
        persistTokenState: Boolean = true,
        persistBotIdentities: Boolean = true,
    ) {
        if (persistTokenState) {
            saveTokens(
                idToken = idToken,
                refreshToken = refreshToken,
                accessToken = accessToken,
                persistBotIdentities = persistBotIdentities,
            )
        }
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
                    trackAndLogoutForTokenExpiry(
                        cause = AuthSessionCause.REFRESH_TOKEN_EXPIRED_OR_INVALID,
                        flow = AuthSessionFlow.TOKEN_VALIDATION,
                    )
                }
            } ?: trackAndLogoutForTokenExpiry(
                cause = AuthSessionCause.REFRESH_TOKEN_MISSING,
                flow = AuthSessionFlow.TOKEN_VALIDATION,
            )
        }
    }

    private fun readCurrentAuthSessionState(): AuthSessionState =
        if (sessionManager.userPrincipal != null) {
            AuthSessionState.AUTHENTICATED
        } else {
            AuthSessionState.UNAUTHENTICATED
        }

    private suspend fun trackAndLogoutForTokenExpiry(
        cause: AuthSessionCause,
        flow: AuthSessionFlow,
    ) {
        authTelemetry.sessionStateChanged(
            fromState = readCurrentAuthSessionState(),
            toState = AuthSessionState.UNAUTHENTICATED,
            initiator = AuthSessionInitiator.SYSTEM,
            cause = cause,
            flow = flow,
        )
        logoutInternal("system_${cause.name}")
    }

    private suspend fun saveTokens(
        idToken: String,
        refreshToken: String,
        accessToken: String,
        persistBotIdentities: Boolean = true,
    ) {
        preferences.putString(PrefKeys.ID_TOKEN.name, idToken)
        if (refreshToken.isNotEmpty()) {
            preferences.putString(PrefKeys.REFRESH_TOKEN.name, refreshToken)
        }
        if (accessToken.isNotEmpty()) {
            preferences.putString(PrefKeys.ACCESS_TOKEN.name, accessToken)
        }
        if (persistBotIdentities) {
            persistBotIdentitiesFromToken(idToken)
            updateBotCountFromPrefs()
        }
    }

    private suspend fun updateBotCountFromPrefs() {
        val raw = preferences.getString(PrefKeys.BOT_IDENTITIES.name)
        val count =
            raw?.let { runCatching { json.decodeFromString<List<BotIdentityEntry>>(it).size }.getOrNull() }
                ?: 0
        Logger.d("BotIdentitySource") { "updateBotCountFromPrefs source=local_pref count=$count" }
        sessionManager.updateBotCount(count)
    }

    override suspend fun refreshTokensAfterBotDeletion() {
        val refreshToken = preferences.getString(PrefKeys.REFRESH_TOKEN.name)
        if (refreshToken.isNullOrBlank()) {
            Logger.w("BotIdentitySource") {
                "refreshTokensAfterBotDeletion skipped: refresh token missing"
            }
            return
        }
        requiredUseCases.refreshTokenUseCase
            .invoke(refreshToken)
            .onSuccess { tokenResponse ->
                saveTokens(
                    idToken = tokenResponse.idToken,
                    refreshToken = tokenResponse.refreshToken,
                    accessToken = tokenResponse.accessToken,
                    persistBotIdentities = true,
                )
            }.onFailure { error ->
                Logger.e("BotIdentitySource") {
                    "refreshTokensAfterBotDeletion failed: ${error.message}"
                }
            }
    }

    override suspend fun logout() {
        logoutInternal("user_logout")
    }

    private suspend fun logoutInternal(analyticsResetReason: String) {
        // clear preferences
        listOf(
            PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name,
            PrefKeys.REFRESH_TOKEN.name,
            PrefKeys.ACCESS_TOKEN.name,
            PrefKeys.ID_TOKEN.name,
            PrefKeys.HOW_TO_PLAY_SHOWN.name,
            PrefKeys.USERNAME.name,
            // PrefKeys.SMILEY_GAME_NUDGE_SHOWN.name,
            PrefKeys.PHONE_NUMBER.name,
            PrefKeys.AI_VIDEO_SUBSCRIPTION_NUDGE_LAST_SHOWN_DATE.name,
            PrefKeys.TOURNAMENT_LEADERBOARD_SUBSCRIPTION_NUDGE_LAST_SHOWN_DATE.name,
        ).forEach { key ->
            preferences.remove(key)
        }
        requiredUseCases.deregisterNotificationTokenUseCase()
        // clear cached canister data after parsing token
        resetCachedCanisterData()
        // reset analytics manage: flush events and reset user properties
        analyticsManager.resetWithReason(analyticsResetReason)
        // reset session manager for properties
        sessionManager.resetSessionProperties()
        // reset crashlytics
        crashlyticsManager.setUserId("")
        // logout of firebase
        requiredUseCases.signOutUseCase.invoke(Unit)
        // set session state to initial for re-login
        sessionManager.updateState(SessionState.Initial)
    }

    private suspend fun refreshAuthenticateWithNetwork(data: ByteArray): Session? =
        try {
            Logger.d("DefaultAuthClient") { "Refreshing authenticateWithNetwork" }
            val canisterWrapper = authenticateWithNetwork(data)
            cacheSession(data, canisterWrapper)
            Logger.d("DefaultAuthClient") { "Reauthenticated: ${canisterWrapper.isCreatedFromServiceCanister} " }
            Session(
                identity = data,
                canisterId = canisterWrapper.canisterId,
                userPrincipal = canisterWrapper.userPrincipalId,
                profilePic = canisterWrapper.profilePic,
                username = resolveUsername(canisterWrapper.username, canisterWrapper.userPrincipalId),
                isCreatedFromServiceCanister = canisterWrapper.isCreatedFromServiceCanister,
            )
        } catch (e: YralFfiException) {
            crashlyticsManager.recordException(
                YralException("Reauthenticate failed $e"),
                ExceptionType.AUTH,
            )
            null
        }

    private suspend fun handleExtractIdentityResponse(data: ByteArray) {
        try {
            var cachedSession = getCachedSession()
            if (cachedSession == null) {
                // Use getSessionFromIdentity to get principal without network call
                // Uses USER_INFO_SERVICE_ID as default canister
                val canisterWrapper = getSessionFromIdentity(data)
                cacheSession(data, canisterWrapper)
                cachedSession =
                    Session(
                        identity = data,
                        canisterId = canisterWrapper.canisterId,
                        userPrincipal = canisterWrapper.userPrincipalId,
                        profilePic = canisterWrapper.profilePic,
                        username = resolveUsername(canisterWrapper.username, canisterWrapper.userPrincipalId),
                        isCreatedFromServiceCanister = canisterWrapper.isCreatedFromServiceCanister,
                    )
            }
            cachedSession.userPrincipal?.let { crashlyticsManager.setUserId(it) }
            sessionManager.updateCoinBalance(0)
            // Always refresh with network to get actual canister and profile info
            val reAuthenticatedSession = refreshAuthenticateWithNetwork(data)
            val finalSession = reAuthenticatedSession ?: cachedSession
            setSession(session = finalSession)
            if (auth.currentUser?.uid == finalSession.userPrincipal) {
                sessionManager.updateFirebaseLoginState(true)
                postFirebaseLogin(finalSession)
            } else {
                sessionManager.updateFirebaseLoginState(false)
            }
        } catch (e: YralFfiException) {
            resetCachedCanisterData()
            crashlyticsManager.recordException(e, ExceptionType.AUTH)
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
        scope.launch {
            session.userPrincipal?.let { userPrincipal ->
                session.username?.let { username ->
                    requiredUseCases.updateDocumentUseCase
                        .invoke(
                            parameter =
                                UpdateDocumentUseCase.Params(
                                    collectionName = "users",
                                    documentId = userPrincipal,
                                    fieldAndValue = Pair("username", username),
                                ),
                        ).onFailure { error ->
                            Logger.e(error) { "Failed to update username for $userPrincipal" }
                        }
                }
            }
        }
        scope.launch { updateBalanceAndProceed(session) }
        scope.launch {
            val result = requiredUseCases.registerNotificationTokenUseCase()
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
                }.onFailure {
                    trackAndLogoutForTokenExpiry(
                        cause = AuthSessionCause.REFRESH_ACCESS_TOKEN_FAILED,
                        flow = AuthSessionFlow.TOKEN_REFRESH,
                    )
                }
        } ?: obtainAnonymousIdentity()
    }

    override suspend fun signInWithSocial(
        context: Any,
        provider: SocialProvider,
    ) {
        currentProvider = provider
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
        var currentUserPrincipal: String? = null
        val provider = currentProvider ?: SocialProvider.GOOGLE
        when (result) {
            is OAuthResult.Success -> {
                if (result.state != currentState) {
                    authTelemetry.authFailed(provider)
                    currentProvider = null
                    throw SecurityException("Invalid state parameter - possible CSRF attack")
                }
                // reset analytics manage: flush events and reset user properties
                analyticsManager.flush()
                sessionManager.canisterID?.let { canisterId ->
                    sessionManager.userPrincipal?.let { userPrincipal ->
                        currentUserPrincipal = userPrincipal
                        analyticsManager.setUserProperties(
                            User(
                                userId = userPrincipal,
                                canisterId = canisterId,
                            ),
                        )
                    }
                }
                sessionManager.updateState(SessionState.Loading)
                authenticate(result.code, currentUserPrincipal)
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
        authTelemetry.authFailed(provider)
        currentProvider = null
        throw YralAuthException("OAuth authentication failed - $error")
    }

    private suspend fun authenticate(
        code: String,
        currentUserPrincipal: String?,
    ) {
        val provider = currentProvider ?: SocialProvider.GOOGLE
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
                preferences.getString(PrefKeys.PHONE_NUMBER.name)?.let { phone ->
                    sessionManager.updatePhoneNumber(phone)
                }
                scope.launch { getCachedSession()?.let { updateYralSession(it) } }
                scope.launch {
                    // Minor delay for super properties to be set
                    delay(DELAY_FOR_SESSION_PROPERTIES)
                    authTelemetry.onAuthSuccess(
                        isNewUser = currentUserPrincipal == sessionManager.userPrincipal,
                        provider = provider,
                    )
                }
                currentProvider = null
            }.onFailure {
                authTelemetry.authFailed(provider)
                currentProvider = null
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
        val phoneAuthLoginUseCase: PhoneAuthLoginUseCase,
        val verifyPhoneAuthUseCase: VerifyPhoneAuthUseCase,
    )

    private suspend fun getCachedSession(): Session? {
        // Prefer explicitly stored main identity/principal when available
        val mainIdentity = preferences.getBytes(PrefKeys.MAIN_IDENTITY.name)
        val mainPrincipal = preferences.getString(PrefKeys.MAIN_PRINCIPAL.name)
        val lastActivePrincipal = preferences.getString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name)

        // If last active was a bot (or non-main), try using the generic identity/principal first
        val preferredIdentity = preferences.getBytes(PrefKeys.IDENTITY.name)
        val preferredPrincipal = preferences.getString(PrefKeys.USER_PRINCIPAL.name)
        val usePreferred =
            lastActivePrincipal != null &&
                preferredPrincipal == lastActivePrincipal &&
                preferredIdentity != null

        val identity = if (usePreferred) preferredIdentity else mainIdentity ?: preferredIdentity
        val canisterId = preferences.getString(PrefKeys.CANISTER_ID.name)
        val userPrincipal = if (usePreferred) preferredPrincipal else mainPrincipal ?: preferredPrincipal
        val profilePic = preferences.getString(PrefKeys.PROFILE_PIC.name)
        val username = preferences.getString(PrefKeys.USERNAME.name)
        val isCreatedFromServiceCanister = preferences.getBoolean(PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name)
        val resolvedIsBotAccount =
            mainPrincipal?.let { main -> userPrincipal != null && userPrincipal != main } ?: false
        return listOf(identity, canisterId, userPrincipal, profilePic)
            .all { it != null }
            .let { allPresent ->
                if (allPresent) {
                    Session(
                        identity = identity!!,
                        canisterId = canisterId!!,
                        userPrincipal = userPrincipal!!,
                        profilePic = profilePic!!,
                        username = resolveUsername(username, userPrincipal),
                        isCreatedFromServiceCanister = isCreatedFromServiceCanister ?: false,
                        isBotAccount = resolvedIsBotAccount,
                    )
                } else {
                    null
                }
            }
    }

    private suspend fun cacheSession(
        identity: ByteArray,
        canisterWrapper: CanisterData,
        isBotAccount: Boolean = false,
    ) {
        with(canisterWrapper) {
            preferences.putBytes(PrefKeys.IDENTITY.name, identity)
            preferences.putString(PrefKeys.CANISTER_ID.name, canisterId)
            preferences.putString(PrefKeys.USER_PRINCIPAL.name, userPrincipalId)
            preferences.putString(PrefKeys.PROFILE_PIC.name, profilePic)
            val resolvedUsername = resolveUsername(username, userPrincipalId)
            if (resolvedUsername != null) {
                preferences.putString(PrefKeys.USERNAME.name, resolvedUsername)
            } else {
                preferences.remove(PrefKeys.USERNAME.name)
            }
            preferences.putBoolean(PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name, isCreatedFromServiceCanister)
            // Always persist a main identity when the session is not a bot account
            if (!isBotAccount) {
                preferences.putBytes(PrefKeys.MAIN_IDENTITY.name, identity)
                preferences.putString(PrefKeys.MAIN_PRINCIPAL.name, userPrincipalId)
                preferences.putString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name, userPrincipalId)
            }
        }
    }

    private suspend fun resetCachedCanisterData() {
        preferences.remove(PrefKeys.IDENTITY.name)
        preferences.remove(PrefKeys.CANISTER_ID.name)
        preferences.remove(PrefKeys.USER_PRINCIPAL.name)
        preferences.remove(PrefKeys.PROFILE_PIC.name)
        preferences.remove(PrefKeys.USERNAME.name)
        preferences.remove(PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name)
        preferences.remove(PrefKeys.BOT_IDENTITIES.name)
        preferences.remove(PrefKeys.ACCOUNT_DIRECTORY_CACHE.name)
    }

    override suspend fun phoneAuthLogin(phoneNumber: String): PhoneAuthLoginResponse {
        val identity =
            sessionManager.identity
                ?: throw YralAuthException("Phone auth login failed - identity not available")
        return requiredUseCases.phoneAuthLoginUseCase
            .invoke(
                PhoneAuthLoginUseCase.Params(
                    phoneNumber = phoneNumber,
                    identity = identity,
                ),
            ).onSuccess { result ->
                Logger.d("DefaultAuthClient") { "Phone auth login initiated for $phoneNumber" }
                currentState = result.codeChallenge
                return result
            }.onFailure { error ->
                Logger.e("DefaultAuthClient") { "Phone auth login failed: ${error.message}" }
                throw YralAuthException("Phone auth login failed - ${error.message}")
            }.getOrThrow()
    }

    override suspend fun verifyPhoneAuth(
        phoneNumber: String,
        code: String,
    ) {
        currentState?.let { currentState ->
            requiredUseCases.verifyPhoneAuthUseCase
                .invoke(
                    VerifyPhoneAuthUseCase.Params(
                        phoneNumber = phoneNumber,
                        code = code,
                        clientState = currentState,
                    ),
                ).onSuccess { response ->
                    Logger.d("DefaultAuthClient") { "Phone auth verification completed" }
                    when (response) {
                        is PhoneAuthVerifyResponse.Error -> {
                            authTelemetry.otpValidationResult(
                                status = OtpValidationStatus.FAILURE,
                                reason = response.error,
                                phoneNumber = phoneNumber,
                            )
                            authTelemetry.authFailed(SocialProvider.PHONE)
                            throw YralAuthException("Phone auth verification failed - ${response.errorMessage}")
                        }
                        is PhoneAuthVerifyResponse.Success -> {
                            authTelemetry.otpValidationResult(
                                status = OtpValidationStatus.SUCCESS,
                                reason = null,
                                phoneNumber = phoneNumber,
                            )
                            preferences.putString(PrefKeys.PHONE_NUMBER.name, phoneNumber)
                            sessionManager.updatePhoneNumber(phoneNumber)
                            val userPrincipal =
                                sessionManager.userPrincipal
                                    ?: throw YralAuthException(
                                        "Phone auth verification failed - user principal not found",
                                    )
                            currentProvider = SocialProvider.PHONE
                            authenticate(response.idTokenCode, userPrincipal)
                        }
                    }
                }.onFailure { error ->
                    authTelemetry.otpValidationResult(
                        status = OtpValidationStatus.FAILURE,
                        reason = error.message,
                        phoneNumber = phoneNumber,
                    )
                    authTelemetry.authFailed(SocialProvider.PHONE)
                    Logger.e("DefaultAuthClient") { "Phone auth verification failed: ${error.message}" }
                    throw YralAuthException("Phone auth verification failed - ${error.message}")
                }.getOrThrow()
        } ?: throw YralAuthException("Phone auth verification failed - no state found")
    }

    @Suppress("LongMethod")
    private suspend fun persistBotIdentitiesFromToken(idToken: String) {
        val claims = runCatching { oAuthUtilsHelper.parseOAuthToken(idToken) }.getOrNull()
        val botIdentities = claims?.botDelegatedIdentities?.takeIf { it.isNotEmpty() }
        if (botIdentities == null) {
            Logger.d("DefaultAuthClient") {
                val payloadKeys = extractPayloadKeys(idToken)
                val payloadJson = extractPayloadJson(idToken)
                "persistBotIdentitiesFromToken: no bot identities in token. " +
                    "Payload keys=$payloadKeys payload=$payloadJson"
            }
        } else {
            Logger.d("DefaultAuthClient") {
                val payloadKeys = extractPayloadKeys(idToken)
                "persistBotIdentitiesFromToken: found ${botIdentities.size} bot identities. " +
                    "Payload keys=$payloadKeys"
            }
            val existing =
                preferences
                    .getString(PrefKeys.BOT_IDENTITIES.name)
                    ?.let { runCatching { json.decodeFromString<List<BotIdentityEntry>>(it) }.getOrNull() }
                    .orEmpty()
            val entries =
                botIdentities
                    .mapNotNull { raw ->
                        runCatching {
                            val decodedString = raw.decodeToString()
                            val wire =
                                runCatching { json.decodeFromString<KotlinDelegatedIdentityWire>(decodedString) }
                                    .getOrElse {
                                        val base64Decoded = decodedString.decodeBase64Bytes()
                                        json.decodeFromString<KotlinDelegatedIdentityWire>(
                                            base64Decoded.decodeToString(),
                                        )
                                    }
                            val encoded = json.encodeToString(wire).encodeToByteArray()
                            val principal = getSessionFromIdentity(encoded).userPrincipalId
                            Logger.d("DefaultAuthClient") {
                                "persistBotIdentitiesFromToken: decoded bot principal $principal"
                            }
                            BotIdentityEntry(principal = principal, identity = encoded.encodeBase64())
                        }.onFailure { error ->
                            Logger.e("DefaultAuthClient") {
                                "Failed to persist bot identity from token: ${error.message}"
                            }
                        }.getOrNull()
                    }.filter { it.principal.isNotBlank() }
            if (entries.isNotEmpty()) {
                val merged =
                    (existing + entries)
                        .groupBy { it.principal }
                        .map { (_, list) ->
                            val latest = list.last()
                            val username =
                                list
                                    .asReversed()
                                    .firstOrNull { !it.username.isNullOrBlank() }
                                    ?.username
                            latest.copy(username = username)
                        }
                preferences.putString(PrefKeys.BOT_IDENTITIES.name, json.encodeToString(merged))
                Logger.d("BotIdentitySource") {
                    "persistBotIdentitiesFromToken source=oauth_token existing=${existing.size} " +
                        "new=${entries.size} merged=${merged.size}"
                }
            }
        }
    }
}

class SecurityException(
    message: String,
) : Exception(message)

@Serializable
private data class BotIdentityEntry(
    val principal: String,
    val identity: String,
    val username: String? = null,
)

private fun extractPayloadKeys(idToken: String): Set<String> {
    val payloadJson = extractPayloadJson(idToken)
    return if (payloadJson == null) {
        emptySet()
    } else {
        runCatching<JsonElement> {
            Json { ignoreUnknownKeys = true }.parseToJsonElement(payloadJson)
        }.map { element -> element.jsonObject.keys }.getOrDefault(emptySet())
    }
}

private fun extractPayloadJson(idToken: String): String? =
    idToken
        .split(".")
        .getOrNull(1)
        ?.let { runCatching { it.decodeBase64Bytes().decodeToString() }.getOrNull() }
