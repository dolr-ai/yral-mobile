package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.FeedFeatureFlags
import com.yral.shared.analytics.AnalyticsUtmParams
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.TokenType
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.hasSameUserPrincipal
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.YralAuthException
import com.yral.shared.features.auth.YralFBAuthException
import com.yral.shared.features.root.analytics.RootTelemetry
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.preferences.stores.UtmAttributionStore
import com.yral.shared.preferences.stores.UtmParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

enum class RootError {
    TIMEOUT,
}

sealed interface NavigationTarget {
    data object Splash : NavigationTarget
    data object Home : NavigationTarget
    data object MandatoryLogin : NavigationTarget
}

@OptIn(ExperimentalTime::class)
@Suppress("TooGenericExceptionCaught")
class RootViewModel(
    appDispatchers: AppDispatchers,
    authClientFactory: AuthClientFactory,
    private val sessionManager: SessionManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val rootTelemetry: RootTelemetry,
    private val flagManager: FeatureFlagManager,
    private val preferences: Preferences,
    private val utmAttributionStore: UtmAttributionStore,
    private val iapManager: IAPManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                // for async calls after setSession
                // updateSessionAsRegistered, FirebaseAuth/Coin balance update
                _state.update { it.copy(error = RootError.TIMEOUT) }
                Logger.e("Auth error - $e")
                crashlyticsManager.recordException(e, ExceptionType.AUTH)
            }

    internal var splashScreenTimeout: Long = SPLASH_SCREEN_TIMEOUT
    internal var initialDelayForSetup: Long = INITIAL_DELAY_FOR_SETUP

    companion object {
        const val SPLASH_SCREEN_TIMEOUT = 31000L // 31 seconds timeout
        const val INITIAL_DELAY_FOR_SETUP = 300L
    }

    private val _state = MutableStateFlow(RootState())
    val state: StateFlow<RootState> = _state.asStateFlow()

    private val analyticsUser =
        sessionManager.observeSessionStateWithProperty { state, properties ->
            sessionManager.userPrincipal?.let { userPrincipal ->
                sessionManager.canisterID?.let { canisterID ->
                    User(
                        userId = userPrincipal,
                        canisterId = canisterID,
                        isLoggedIn = properties.isSocialSignIn,
                        isCreator = properties.profileVideosCount?.let { it > 0 },
                        walletBalance = properties.coinBalance?.toDouble(),
                        tokenType = TokenType.YRAL,
                        emailId = properties.emailId,
                        utmParams = utmAttributionStore.get()?.toAnalyticsUtmParams(),
                        isMandatoryLogin = properties.isMandatoryLogin,
                        phoneNumber = properties.phoneNumber,
                    )
                }
            }
        }

    private var initialisationJob: Job? = null
    private var firebaseJob: Job? = null

    init {
        coroutineScope.launch {
            sessionManager
                .observeSessionState(transform = { it })
                .collect { sessionState ->
                    if (sessionState != _state.value.sessionState) {
                        // session may be updated with user profile details while principal remains same
                        val isSessionPrincipalSame = sessionState.hasSameUserPrincipal(_state.value.sessionState)
                        _state.update {
                            it.copy(
                                sessionState = sessionState,
                                isPendingLogin =
                                    if (isSessionPrincipalSame) {
                                        it.isPendingLogin
                                    } else {
                                        UiState.InProgress()
                                    },
                            )
                        }
                        when (sessionState) {
                            is SessionState.Initial -> initialize()
                            is SessionState.SignedIn -> initialize(isSessionPrincipalSame)
                            else -> Unit
                        }
                    }
                }
        }
        coroutineScope.launch {
            analyticsUser.collect { user -> rootTelemetry.setUser(user) }
        }
        coroutineScope.launch {
            if (preferences.getString(PrefKeys.FIRST_APP_OPEN_DATE_TIME.name) == null) {
                val now = Clock.System.now()
                rootTelemetry.onFirstAppLaunch(now)
                preferences.putString(PrefKeys.FIRST_APP_OPEN_DATE_TIME.name, now.toString())
            }
        }
    }

    fun initialize(isSessionPrincipalSame: Boolean = false) {
        initialisationJob?.cancel()
        initialisationJob =
            coroutineScope.launch {
                _state.update { it.copy(error = null) }
                try {
                    withTimeout(splashScreenTimeout) {
                        checkLoginAndInitialize(isSessionPrincipalSame)
                    }
                } catch (e: TimeoutCancellationException) {
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Splash timeout - ${e.message}")
                    crashlyticsManager.recordException(
                        YralException("Splash screen timeout - initialization took too long"),
                        ExceptionType.AUTH,
                    )
                    throw e
                } catch (e: YralAuthException) {
                    // for async calls before setSession
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Auth error - $e")
                    crashlyticsManager.recordException(e, ExceptionType.AUTH)
                }
            }
    }

    private suspend fun checkLoginAndInitialize(isSessionPrincipalSame: Boolean) {
        delay(initialDelayForSetup)
        sessionManager.identity?.let {
            sessionManager.updateIsForcedGamePlayUser(
                isForcedGamePlayUser = flagManager.isEnabled(FeedFeatureFlags.SmileyGame.StopAndVoteNudge),
            )
            sessionManager.updateIsAutoScrolledEnabled(
                isAutoScrollEnabled = flagManager.isEnabled(FeedFeatureFlags.SmileyGame.AutoScrollEnabled),
            )
            sessionManager.updateSocialSignInStatus(
                isSocialSignIn = preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false,
            )
            if (!isSessionPrincipalSame) {
                resolveNavigationTarget()
                initializeFirebase()
                // Not used as of now we will get details from canister in profileDetailsV6
                // restorePurchases()
            }
        } ?: authClient.initialize()
    }

    private suspend fun initializeFirebase() {
        val isFirebaseAuthenticated =
            sessionManager.readLatestSessionPropertyWithDefault(
                selector = { it.isFirebaseLoggedIn },
                defaultValue = false,
            )
        if (isFirebaseAuthenticated || isPendingLogin()) return
        firebaseJob?.cancel()
        val session = (_state.value.sessionState as? SessionState.SignedIn)?.session ?: return
        firebaseJob =
            coroutineScope.launch {
                try {
                    authClient.fetchBalance(session)
                    authClient.authorizeFirebase(session)
                } catch (e: YralFBAuthException) {
                    // Do not update error in state since no error message required
                    Logger.e("Firebase Auth error - $e")
                    crashlyticsManager.recordException(e, ExceptionType.AUTH)
                } catch (e: YralAuthException) {
                    // can be triggered in postFirebaseLogin when getting balance
                    Logger.e("Fetch Balance error - $e")
                    crashlyticsManager.recordException(e, ExceptionType.AUTH)
                }
            }
    }

    @Suppress("UnusedPrivateMember")
    private fun restorePurchases() {
        coroutineScope.launch {
            val isSocialSignIn =
                sessionManager.readLatestSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                )
            if (!isSocialSignIn) return@launch
            iapManager
                .isProductPurchased(ProductId.YRAL_PRO)
                .fold(
                    onSuccess = { isPurchased -> Logger.d("SubscriptionX") { "isPurchased: $isPurchased" } },
                    onFailure = { Logger.e("SubscriptionX", it) { "Failed to restore" } },
                )
        }
    }

    private suspend fun resolveNavigationTarget() {
        // Wait for remote config and check MandatoryLogin flag
        val isSocialSignedIn = preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false
        if (preferences.getBoolean(PrefKeys.IS_REMOTE_CONFIG_FORCE_SYNCED.name) != true) {
            val fetched = flagManager.awaitRemoteFetch(5.seconds)
            if (fetched) {
                preferences.putBoolean(PrefKeys.IS_REMOTE_CONFIG_FORCE_SYNCED.name, true)
            }
        }
        val isMandatoryLoginEnabled = flagManager.isEnabled(AppFeatureFlags.Common.MandatoryLogin)
        sessionManager.updateIsMandatoryLogin(isMandatoryLoginEnabled)
        val navigationTarget =
            if (isMandatoryLoginEnabled) {
                if (!isSocialSignedIn) {
                    NavigationTarget.MandatoryLogin to true
                } else {
                    NavigationTarget.Home to false
                }
            } else {
                NavigationTarget.Home to false
            }
        _state.update {
            it.copy(
                navigationTarget = navigationTarget.first,
                isLoginMandatory = navigationTarget.second,
                isPendingLogin = UiState.Success(navigationTarget.second && !isSocialSignedIn),
            )
        }
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

    private fun UtmParams.toAnalyticsUtmParams(): AnalyticsUtmParams =
        AnalyticsUtmParams(
            source = source,
            medium = medium,
            campaign = campaign,
            term = term,
            content = content,
        )

    fun isPendingLogin(): Boolean =
        with(_state.value) {
            isPendingLogin !is UiState.Success || isPendingLogin.data
        }
}

data class RootState(
    val initialAnimationComplete: Boolean = false,
    val sessionState: SessionState = SessionState.Loading,
    val error: RootError? = null,
    val navigationTarget: NavigationTarget = NavigationTarget.Splash,
    val isLoginMandatory: Boolean = false,
    val isPendingLogin: UiState<Boolean> = UiState.Initial,
)
