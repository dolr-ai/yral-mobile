package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.YralAuthException
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

enum class RootError {
    TIMEOUT,
}

@Suppress("TooGenericExceptionCaught")
class RootViewModel(
    appDispatchers: AppDispatchers,
    authClientFactory: AuthClientFactory,
    private val sessionManager: SessionManager,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

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

    private var initialisationJob: Job? = null

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
        sessionManager.getIdentity()?.let {
            _state.update { it.copy(showSplash = false) }
        } ?: authClient.initialize()
    }

    fun onSplashAnimationComplete() {
        coroutineScope.launch {
            _state.update { it.copy(initialAnimationComplete = true) }
        }
    }

    fun updateCurrentTab(tab: String) {
        coroutineScope.launch {
            _state.update { it.copy(currentHomePageTab = tab) }
        }
    }
}

data class RootState(
    val showSplash: Boolean = true,
    val initialAnimationComplete: Boolean = false,
    val currentHomePageTab: String = "Home",
    val currentSessionState: SessionState? = null,
    val error: RootError? = null,
)
