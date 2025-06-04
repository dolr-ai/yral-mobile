package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.koin.koinInstance
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.services.IndividualUserServiceFactory
import kotlinx.coroutines.CancellationException
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
import org.koin.core.parameter.parametersOf

enum class RootError {
    TIMEOUT,
    SESSION_INITIALIZATION_FAILED,
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
                    it.copy(
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
                    crashlyticsManager.recordException(
                        YralException("Splash screen timeout - initialization took too long"),
                    )
                    throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    crashlyticsManager.recordException(e)
                    _state.update { it.copy(error = RootError.SESSION_INITIALIZATION_FAILED) }
                }
            }
    }

    private suspend fun checkLoginAndInitialize() {
        delay(INITIAL_DELAY_FOR_SETUP)
        if (sessionManager.getCanisterPrincipal() != null) {
            _state.update { it.copy(showSplash = false) }
        } else {
            authClient.initialize()
            sessionManager.getCanisterPrincipal()?.let { principal ->
                sessionManager.getIdentity()?.let { identity ->
                    individualUserServiceFactory.initialize(
                        principal = principal,
                        identityData = identity,
                    )
                } ?: throw YralException("Identity is null")
            } ?: throw YralException("Principal is null after initialization")
        }
    }

    fun onSplashAnimationComplete() {
        coroutineScope.launch {
            _state.update { it.copy(initialAnimationComplete = true) }
        }
    }

    fun createFeedViewModel(): FeedViewModel =
        koinInstance.get<FeedViewModel> {
            parametersOf(_state.value.posts, _state.value.feedDetails)
        }

    fun updateCurrentTab(tab: String) {
        coroutineScope.launch {
            _state.update { it.copy(currentHomePageTab = tab) }
        }
    }
}

data class RootState(
    val posts: List<Post> = emptyList(),
    val feedDetails: List<FeedDetails> = emptyList(),
    val showSplash: Boolean = true,
    val initialAnimationComplete: Boolean = false,
    val currentHomePageTab: String = "Home",
    val currentSessionState: SessionState? = null,
    val error: RootError? = null,
)
