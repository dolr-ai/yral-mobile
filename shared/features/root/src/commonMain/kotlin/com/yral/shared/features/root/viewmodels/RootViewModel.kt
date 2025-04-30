package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.dispatchers.AppDispatchers
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

@Suppress("TooGenericExceptionCaught")
class RootViewModel(
    appDispatchers: AppDispatchers,
    private val authClient: AuthClient,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val getInitialFeedUseCase: GetInitialFeedUseCase,
    private val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(appDispatchers.io)

    companion object {
        const val MIN_REQUIRED_ITEMS = 1
    }

    private val _state = MutableStateFlow(RootState())
    val state: StateFlow<RootState> = _state.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        coroutineScope.launch {
            if (authClient.canisterPrincipal == null) {
                try {
                    authClient.initialize()
                    authClient.canisterPrincipal?.let { principal ->
                        authClient.identity?.let { identity ->
                            individualUserServiceFactory.initialize(
                                principal = principal,
                                identityData = identity,
                            )
                            initialFeedData(principal)
                        } ?: error("Identity is null")
                    } ?: error("Principal is null after initialization")
                } catch (e: Exception) {
                    crashlyticsManager.recordException(e)
                }
            } else {
                initialFeedData(authClient.canisterPrincipal!!)
            }
        }
    }

    private suspend fun initialFeedData(principal: String) {
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
                    _state.emit(
                        _state.value.copy(
                            posts = posts,
                        ),
                    )
                    if (posts.isNotEmpty()) {
                        posts.take(MIN_REQUIRED_ITEMS).forEach { post -> fetchFeedDetail(post) }
                    }
                },
                failure = { error ->
                    error("Error loading initial posts: $error")
                },
            )
    }

    private suspend fun fetchFeedDetail(post: Post) {
        fetchFeedDetailsUseCase
            .invoke(post)
            .mapBoth(
                success = { detail ->
                    val feedDetailsList = _state.value.feedDetails.toMutableList()
                    feedDetailsList.add(detail)
                    _state.emit(
                        _state.value.copy(
                            feedDetails = feedDetailsList.toList(),
                            showSplash = feedDetailsList.size < MIN_REQUIRED_ITEMS,
                        ),
                    )
                },
                failure = { error ->
                    error("Error loading feed details: $error")
                },
            )
    }

    fun onSplashAnimationComplete() {
        coroutineScope.launch {
            _state.emit(_state.value.copy(initialAnimationComplete = true))
        }
    }

    fun createFeedViewModel(): FeedViewModel =
        koinInstance.get<FeedViewModel> {
            parametersOf(_state.value.posts, _state.value.feedDetails)
        }

    fun updateCurrentTab(tab: String) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    currentHomePageTab = tab,
                ),
            )
        }
    }
}

data class RootState(
    val posts: List<Post> = emptyList(),
    val feedDetails: List<FeedDetails> = emptyList(),
    val showSplash: Boolean = true,
    val initialAnimationComplete: Boolean = false,
    val currentHomePageTab: String = "Home",
)
