package com.yral.shared.features.feed.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.VideoDurationWatchedEventData
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.feed.data.toVideoEventData
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
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
    private val analyticsManager: AnalyticsManager,
    private val preferences: Preferences,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(appDispatchers.io)

    companion object {
        const val PRE_FETCH_BEFORE_LAST = 1
        private const val FIRST_SECOND_WATCHED_THRESHOLD_MS = 1000L
        private const val FULL_VIDEO_WATCHED_THRESHOLD = 95.0
        const val NSFW_PROBABILITY = 0.4
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
                failure = { _ ->
                    // No need to throw error, BaseUseCase reports to CrashlyticsManager
                    // error("Error loading initial posts: $error")
                },
            )
    }

    fun loadMoreFeed() {
        if (_state.value.isLoadingMore) {
            return
        }
        coroutineScope.launch {
            sessionManager.getCanisterPrincipal()?.let {
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
                            setLoadingMore(false)
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
                        failure = { _ ->
                            setLoadingMore(false)
                        },
                    )
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
                    videoData = VideoData(), // Reset all video data for new page
                ),
            )
        }
    }

    fun setPostDescriptionExpanded(isExpanded: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    isPostDescriptionExpanded = isExpanded,
                ),
            )
        }
    }

    fun recordTime(
        currentTime: Int,
        totalTime: Int,
    ) {
        coroutineScope.launch {
            // If we've already logged full video watched, no need to continue processing
            if (_state.value.videoData.didLogFullVideoWatched) {
                return@launch
            }
            // Get current state values before any updates
            val videoData = _state.value.videoData
            // Check for threshold crossing - only if not the first update and we haven't logged it yet
            val shouldLogFirstSecond =
                !videoData.isFirstTimeUpdate &&
                    !videoData.didLogFirstSecondWatched &&
                    currentTime >= FIRST_SECOND_WATCHED_THRESHOLD_MS
            val shouldLogFullVideo =
                currentTime.percentageOf(totalTime) >= FULL_VIDEO_WATCHED_THRESHOLD &&
                    !videoData.didLogFullVideoWatched
            // Update the last known time values
            _state.emit(
                _state.value.copy(
                    videoData =
                        _state.value.videoData.copy(
                            lastKnownCurrentTime = currentTime,
                            lastKnownTotalTime = totalTime,
                            isFirstTimeUpdate = false, // Mark that we've had at least one update
                        ),
                ),
            )
            // Log first second event if needed
            if (shouldLogFirstSecond) {
                recordEvent(
                    videoData =
                        _state.value.videoData.copy(
                            didLogFirstSecondWatched = true,
                        ),
                )
            } else if (shouldLogFullVideo) {
                recordEvent(
                    videoData =
                        state.value.videoData.copy(
                            didLogFullVideoWatched = true,
                        ),
                )
            }
        }
    }

    private suspend fun getVideoEvent(
        currentTime: Int,
        totalTime: Int,
        percentageWatched: Double,
    ): VideoDurationWatchedEventData {
        val currentFeed = _state.value.feedDetails[_state.value.currentPageOfFeed]
        return currentFeed.toVideoEventData().copy(
            canisterId = sessionManager.getCanisterPrincipal() ?: "",
            userID = sessionManager.getUserPrincipal() ?: "",
            isLoggedIn = preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false,
            absoluteWatched = currentTime.toDouble(),
            percentageWatched = percentageWatched,
            videoDuration = totalTime.toDouble(),
        )
    }

    private suspend fun recordEvent(videoData: VideoData) {
        val videoEvent =
            getVideoEvent(
                currentTime = videoData.lastKnownCurrentTime,
                totalTime = videoData.lastKnownTotalTime,
                percentageWatched =
                    videoData.lastKnownCurrentTime
                        .percentageOf(videoData.lastKnownTotalTime),
            )
        analyticsManager.trackEvent(
            event = videoEvent,
        )
        _state.emit(
            _state.value.copy(
                videoData = videoData,
            ),
        )
    }

    fun didCurrentVideoEnd() {
        coroutineScope.launch {
            if (!_state.value.videoData.didLogFullVideoWatched &&
                _state.value.videoData.lastKnownTotalTime > 0
            ) {
                recordEvent(
                    videoData =
                        _state.value.videoData.copy(
                            didLogFullVideoWatched = true,
                            lastKnownCurrentTime = _state.value.videoData.lastKnownTotalTime,
                        ),
                )
            }
        }
    }
}

data class FeedState(
    val posts: List<Post> = emptyList(),
    val feedDetails: List<FeedDetails> = emptyList(),
    val currentPageOfFeed: Int = 0,
    val isLoadingMore: Boolean = false,
    val isPostDescriptionExpanded: Boolean = false,
    val videoData: VideoData = VideoData(),
)

data class VideoData(
    val didLogFirstSecondWatched: Boolean = false,
    val didLogFullVideoWatched: Boolean = false,
    val lastKnownCurrentTime: Int = 0,
    val lastKnownTotalTime: Int = 0,
    val isFirstTimeUpdate: Boolean = true,
)

/**
 * Extension function to calculate percentage of a value relative to total
 * @return Percentage value (0-100)
 */
@Suppress("MagicNumber")
private fun Int.percentageOf(total: Int): Double =
    if (total > 0) {
        (this.toDouble() / total.toDouble()) * 100.0
    } else {
        0.0
    }
