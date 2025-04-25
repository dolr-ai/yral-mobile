package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.domain.models.toFilteredResult
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.propicFromPrincipal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("TooGenericExceptionCaught")
class RootViewModel(
    private val appDispatchers: AppDispatchers,
    private val authClient: AuthClient,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val getInitialFeedUseCase: GetInitialFeedUseCase,
    private val fetchMoreFeedUseCase: FetchMoreFeedUseCase,
    private val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

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
                withContext(appDispatchers.io) {
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
                        posts.forEach { post -> fetchFeedDetail(post) }
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

    fun loadMoreFeed() {
        if (_state.value.isLoadingMore) {
            return
        }
        coroutineScope.launch {
            if (authClient.canisterPrincipal != null) {
                try {
                    setLoadingMore(true)
                    fetchMoreFeedUseCase
                        .invoke(
                            parameter =
                                FetchMoreFeedUseCase.Params(
                                    canisterID = authClient.canisterPrincipal!!,
                                    filterResults =
                                        _state.value.posts.map { post ->
                                            post.toFilteredResult()
                                        },
                                ),
                        ).mapBoth(
                            success = { moreFeed ->
                                val posts = _state.value.posts.toMutableList()
                                posts.addAll(moreFeed.posts)
                                _state.emit(
                                    _state.value.copy(
                                        posts = posts,
                                    ),
                                )
                                moreFeed.posts.forEach { post ->
                                    fetchFeedDetail(post)
                                }
                            },
                            failure = { println("xxxx error: $it") },
                        )
                    setLoadingMore(false)
                } catch (e: Exception) {
                    setLoadingMore(false)
                    crashlyticsManager.recordException(e)
                }
            }
        }
    }

    private suspend fun setLoadingMore(isLoading: Boolean) {
        _state.emit(
            _state.value.copy(
                isLoadingMore = isLoading,
            ),
        )
    }

    fun onCurrentPageChange(pageNo: Int) {
        coroutineScope.launch {
            withContext(appDispatchers.io) {
                _state.emit(
                    _state.value.copy(
                        currentPageOfFeed = pageNo,
                    ),
                )
            }
        }
    }

    fun getAccountInfo(): AccountInfo? {
        val canisterPrincipal = authClient.canisterPrincipal
        val userPrincipal = authClient.userPrincipal
        canisterPrincipal?.let { principal ->
            userPrincipal?.let { userPrincipal ->
                return AccountInfo(
                    profilePic = propicFromPrincipal(principal),
                    userPrincipal = userPrincipal,
                )
            }
        }
        return null
    }
}

data class RootState(
    val posts: List<Post> = emptyList(),
    val feedDetails: List<FeedDetails> = emptyList(),
    val currentPageOfFeed: Int = 0,
    val showSplash: Boolean = true,
    val initialAnimationComplete: Boolean = false,
    val isLoadingMore: Boolean = false,
)

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
)
