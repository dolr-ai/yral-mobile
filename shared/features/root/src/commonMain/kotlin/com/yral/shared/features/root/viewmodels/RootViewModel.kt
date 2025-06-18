package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.FfiException
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
    private val authClient: AuthClient,
    private val sessionManager: SessionManager,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

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
                _state.update {
                    RootState(
                        currentSessionState = sessionManager.state.value,
                        currentHomePageTab = it.currentHomePageTab,
                        initialAnimationComplete = it.initialAnimationComplete,
                        error = null,
                    )
                }
                try {
                    withTimeout(SPLASH_SCREEN_TIMEOUT) {
                        checkLoginAndInitialize()
                    }
                } catch (e: TimeoutCancellationException) {
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Splash timeout - ${e.message}")
                    crashlyticsManager.recordException(
                        YralException("Splash screen timeout - initialization took too long"),
                    )
                    throw e
                } catch (e: YralException) {
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Splash screen error - ${e.message}")
                    crashlyticsManager.recordException(
                        YralException("Splash screen error - ${e.message}"),
                    )
                } catch (e: FfiException) {
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Splash screen on chain error - ${e.message}")
                    crashlyticsManager.recordException(
                        YralException("Splash screen on chain error - ${e.message}"),
                    )
                }
            }
    }

    private suspend fun checkLoginAndInitialize() {
        delay(INITIAL_DELAY_FOR_SETUP)
        sessionManager.getCanisterPrincipal()?.let { principal ->
            sessionManager.getIdentity()?.let { identity ->
                individualUserServiceFactory.initialize(
                    principal = principal,
                    identityData = identity,
                )
                _state.update { it.copy(showSplash = false) }
            } ?: authClient.initialize()
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
