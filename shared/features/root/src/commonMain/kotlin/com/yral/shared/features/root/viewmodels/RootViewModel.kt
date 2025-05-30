package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.koin.koinInstance
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.services.IndividualUserServiceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

enum class RootError {
    TIMEOUT,
    SESSION_INITIALIZATION_FAILED,
    INITIAL_CONTENT_FAILED,
    FEED_DETAILS_FAILED,
    UNEXPECTED_ERROR,
}

@Suppress("TooGenericExceptionCaught")
class RootViewModel(
    appDispatchers: AppDispatchers,
    private val authClient: AuthClient,
    private val sessionManager: SessionManager,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val getInitialFeedUseCase: GetInitialFeedUseCase,
    private val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

    companion object {
        const val MIN_REQUIRED_ITEMS = 1
        private const val SPLASH_SCREEN_TIMEOUT = 20000L // 20 seconds timeout
        private const val INITIAL_DELAY_FOR_SETUP = 300L
    }

    private val _state = MutableStateFlow(RootState())
    val state: StateFlow<RootState> = _state.asStateFlow()
    val sessionManagerState = sessionManager.state

    fun initialize() {
        coroutineScope.launch {
            _state.update {
                it.copy(
                    currentSessionState = sessionManager.state.value,
                    currentHomePageTab = it.currentHomePageTab,
                    initialAnimationComplete = it.initialAnimationComplete,
                    error = null,
                )
            }
            // Launch timeout coroutine
            launch {
                delay(SPLASH_SCREEN_TIMEOUT)
                if (_state.value.showSplash) {
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    crashlyticsManager.recordException(
                        YralException("Splash screen timeout - initialization took too long"),
                    )
                }
            }

            try {
                delay(INITIAL_DELAY_FOR_SETUP)
                if (sessionManager.getCanisterPrincipal() != null) {
                    initialFeedData(sessionManager.getUserPrincipal()!!)
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
            } catch (e: Exception) {
                crashlyticsManager.recordException(e)
                _state.update { it.copy(error = RootError.SESSION_INITIALIZATION_FAILED) }
            }
        }
    }

    private suspend fun initialFeedData(principal: String) {
        try {
            getInitialFeedUseCase
                .invoke(
                    parameter =
                        GetInitialFeedUseCase.Params(
                            canisterID = principal,
                            filterResults = emptyList(),
                        ),
                ).mapBoth(
                    success = { result ->
                        val posts = result.posts
                        _state.update { it.copy(posts = posts) }
                        if (posts.isNotEmpty()) {
                            posts.take(MIN_REQUIRED_ITEMS).forEach { post -> fetchFeedDetail(post) }
                        } else {
                            // No posts available, but session is initialized
                            _state.update { it.copy(showSplash = false) }
                        }
                    },
                    failure = { _ ->
                        // UseCase already records the exception
                        _state.update { it.copy(error = RootError.INITIAL_CONTENT_FAILED) }
                    },
                )
        } catch (e: Exception) {
            crashlyticsManager.recordException(e)
            _state.update { it.copy(error = RootError.UNEXPECTED_ERROR) }
        }
    }

    private suspend fun fetchFeedDetail(post: Post) {
        try {
            fetchFeedDetailsUseCase
                .invoke(post)
                .mapBoth(
                    success = { detail ->
                        updateFeedDetailsState(detail)
                    },
                    failure = { _ ->
                        // UseCase already records the exception
                        _state.update { it.copy(error = RootError.FEED_DETAILS_FAILED) }
                    },
                )
        } catch (e: Exception) {
            crashlyticsManager.recordException(e)
            _state.update { it.copy(error = RootError.UNEXPECTED_ERROR) }
        }
    }

    private fun updateFeedDetailsState(newDetail: FeedDetails) {
        coroutineScope.launch {
            _state.update { currentState ->
                val feedDetailsList = currentState.feedDetails.toMutableList()
                feedDetailsList.add(newDetail)
                currentState.copy(
                    feedDetails = feedDetailsList.toList(),
                    showSplash = feedDetailsList.size < MIN_REQUIRED_ITEMS,
                )
            }
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
