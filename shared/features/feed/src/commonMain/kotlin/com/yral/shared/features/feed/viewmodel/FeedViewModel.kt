package com.yral.shared.features.feed.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.domain.models.toFilteredResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooGenericExceptionCaught")
class FeedViewModel(
    initialPosts: List<Post>,
    initialFeedDetails: List<FeedDetails>,
    appDispatchers: AppDispatchers,
    private val sessionManager: SessionManager,
    private val fetchMoreFeedUseCase: FetchMoreFeedUseCase,
    private val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(appDispatchers.io)

    companion object {
        const val PRE_FETCH_BEFORE_LAST = 1
    }

    private val _state =
        MutableStateFlow(
            FeedState(
                posts = initialPosts,
                feedDetails = initialFeedDetails,
            ),
        )
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            if (_state.value.posts.isEmpty()) {
                loadMoreFeed()
            } else {
                _state.value.posts
                    .filter { post ->
                        _state.value.feedDetails.any { feedDetails -> feedDetails.videoID != post.videoID }
                    }.forEach {
                        fetchFeedDetail(it)
                    }
            }
        }
    }

    private suspend fun fetchFeedDetail(post: Post) {
        try {
            fetchFeedDetailsUseCase
                .invoke(post)
                .mapBoth(
                    success = { detail ->
                        val feedDetailsList = _state.value.feedDetails.toMutableList()
                        feedDetailsList.add(detail)
                        _state.emit(
                            _state.value.copy(
                                feedDetails = feedDetailsList.toList(),
                            ),
                        )
                    },
                    failure = { error ->
                        error("Error loading feed details: $error")
                    },
                )
        } catch (e: Exception) {
            crashlyticsManager.recordException(e)
        }
    }

    fun loadMoreFeed() {
        if (_state.value.isLoadingMore) {
            return
        }
        coroutineScope.launch {
            sessionManager.getCanisterPrincipal()?.let {
                try {
                    setLoadingMore(true)
                    fetchMoreFeedUseCase
                        .invoke(
                            parameter =
                                FetchMoreFeedUseCase.Params(
                                    canisterID = it,
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
                            failure = { error ->
                                error("Error loading more feed: $error")
                            },
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
            _state.emit(
                _state.value.copy(
                    currentPageOfFeed = pageNo,
                ),
            )
        }
    }
}

data class FeedState(
    val posts: List<Post> = emptyList(),
    val feedDetails: List<FeedDetails> = emptyList(),
    val currentPageOfFeed: Int = 0,
    val isLoadingMore: Boolean = false,
)
