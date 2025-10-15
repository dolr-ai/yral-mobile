package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.FeedFeatureFlags
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.TokenType
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.YralAuthException
import com.yral.shared.features.auth.YralFBAuthException
import com.yral.shared.features.root.analytics.RootTelemetry
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class RootError {
    TIMEOUT,
}

@Suppress("TooGenericExceptionCaught")
class RootViewModel(
    appDispatchers: AppDispatchers,
    authClientFactory: AuthClientFactory,
    private val sessionManager: SessionManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val rootTelemetry: RootTelemetry,
    private val flagManager: FeatureFlagManager,
    private val preferences: Preferences,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                // for async calls after setSession
                // updateSessionAsRegistered, FirebaseAuth/Coin balance update
                _state.update { it.copy(error = RootError.TIMEOUT) }
                Logger.e("Auth error - $e")
                crashlyticsManager.recordException(e)
            }

    internal var splashScreenTimeout: Long = SPLASH_SCREEN_TIMEOUT
    internal var initialDelayForSetup: Long = INITIAL_DELAY_FOR_SETUP

    companion object {
        const val SPLASH_SCREEN_TIMEOUT = 20000L // 20 seconds timeout
        const val INITIAL_DELAY_FOR_SETUP = 300L
    }

    private val _state = MutableStateFlow(RootState())
    val state: StateFlow<RootState> = _state.asStateFlow()

    val sessionManagerState = sessionManager.state
    val analyticsUser =
        combine(
            sessionManager.state,
            sessionManager.observeSessionProperties(),
        ) { state, properties ->
            sessionManager.userPrincipal?.let { userPrincipal ->
                sessionManager.canisterID?.let { canisterID ->
                    User(
                        userId = userPrincipal,
                        canisterId = canisterID,
                        isLoggedIn = properties.isSocialSignIn,
                        isCreator = properties.profileVideosCount?.let { it > 0 },
                        walletBalance = properties.coinBalance?.toDouble(),
                        tokenType = TokenType.YRAL,
                        isForcedGamePlayUser = properties.isForcedGamePlayUser,
                        emailId = properties.emailId,
                    )
                }
            }
        }

    val firebaseAuthFlow =
        combine(
            sessionManager.state,
            sessionManager.observeSessionProperties(),
        ) { state, properties ->
            (state is SessionState.SignedIn) to properties.isFirebaseLoggedIn
        }.distinctUntilChanged()

    private var initialisationJob: Job? = null

    init {
        coroutineScope.launch {
            firebaseAuthFlow.collect { (isSignedIn, isAuthenticated) ->
                if (isSignedIn && !isAuthenticated) {
                    coroutineScope.launch {
                        val session = (sessionManager.state.value as? SessionState.SignedIn)?.session
                        try {
                            session?.let {
                                authClient.fetchBalance(session)
                                authClient.authorizeFirebase(session)
                            }
                        } catch (e: YralFBAuthException) {
                            // Do not update error in state since no error message required
                            Logger.e("Firebase Auth error - $e")
                            crashlyticsManager.recordException(e)
                        } catch (e: YralAuthException) {
                            // can be triggered in postFirebaseLogin when getting balance
                            Logger.e("Fetch Balance error - $e")
                            crashlyticsManager.recordException(e)
                        }
                    }
                }
            }
        }
    }

    fun initialize() {
        initialisationJob?.cancel()
        initialisationJob =
            coroutineScope.launch {
                _state.update { it.copy(error = null) }
                try {
                    withTimeout(splashScreenTimeout) {
                        checkLoginAndInitialize()
                    }
                } catch (e: TimeoutCancellationException) {
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Splash timeout - ${e.message}")
                    crashlyticsManager.recordException(
                        YralException("Splash screen timeout - initialization took too long"),
                    )
                    throw e
                } catch (e: YralAuthException) {
                    // for async calls before setSession
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Auth error - $e")
                    crashlyticsManager.recordException(e)
                }
            }
    }

    private suspend fun checkLoginAndInitialize() {
        delay(initialDelayForSetup)
        sessionManager.identity?.let {
            _state.update { it.copy(showSplash = false) }
            sessionManager.updateIsForcedGamePlayUser(
                isForcedGamePlayUser = flagManager.isEnabled(FeedFeatureFlags.SmileyGame.StopAndVoteNudge),
            )
            sessionManager.updateSocialSignInStatus(
                isSocialSignIn = preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false,
            )
        } ?: authClient.initialize()
    }

    fun onSplashAnimationComplete() {
        _state.update { it.copy(initialAnimationComplete = true) }
    }

    fun updateProfileVideosCount(count: Int) {
        sessionManager.updateProfileVideosCount(count)
    }

    fun splashScreenViewed() {
        rootTelemetry.onSplashScreenViewed()
    }

    fun bottomNavigationClicked(categoryName: CategoryName) {
        rootTelemetry.bottomNavigationClicked(categoryName)
    }

    fun setUser(user: User?) {
        rootTelemetry.setUser(user)
    }
}

data class RootState(
    val showSplash: Boolean = true,
    val initialAnimationComplete: Boolean = false,
    val currentSessionState: SessionState? = null,
    val error: RootError? = null,
)
