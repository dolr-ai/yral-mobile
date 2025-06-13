package com.yral.shared.features.feed.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.EmptyColdStartFeedEvent
import com.yral.shared.analytics.events.VideoDurationWatchedEventData
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.feed.data.models.toVideoEventData
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.features.feed.useCases.ReportRequestParams
import com.yral.shared.features.feed.useCases.ReportVideoUseCase
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.domain.models.toFilteredResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooGenericExceptionCaught")
class FeedViewModel(
    initialPosts: List<Post>,
    initialFeedDetails: List<FeedDetails>,
    appDispatchers: AppDispatchers,
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val requiredUseCases: RequiredUseCases,
    private val preferences: Preferences,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

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
                initialFeedData()
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

    private suspend fun initialFeedData() {
        sessionManager.getCanisterPrincipal()?.let { principal ->
            requiredUseCases.getInitialFeedUseCase
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
                            posts.forEach { post -> fetchFeedDetail(post) }
                        } else {
                            analyticsManager.trackEvent(EmptyColdStartFeedEvent())
                            loadMoreFeed()
                        }
                    },
                    failure = { _ ->
                        loadMoreFeed()
                    },
                )
        } ?: crashlyticsManager.recordException(
            YralException("Principal is null while fetching initial feed"),
        )
    }

    private suspend fun fetchFeedDetail(post: Post) {
        requiredUseCases.fetchFeedDetailsUseCase
            .invoke(post)
            .mapBoth(
                success = { detail ->
                    val feedDetailsList = _state.value.feedDetails.toMutableList()
                    feedDetailsList.add(detail)
                    _state.update {
                        it.copy(
                            feedDetails = feedDetailsList.toList(),
                        )
                    }
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
                requiredUseCases.fetchMoreFeedUseCase
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
                            val currentPosts = _state.value.posts
                            val existingIds = currentPosts.map { post -> post.videoID }.toHashSet()
                            val filteredPosts =
                                moreFeed
                                    .posts
                                    .filter { post -> post.videoID !in existingIds }
                            _state.update { currentState ->
                                currentState.copy(
                                    posts = currentState.posts + filteredPosts,
                                )
                            }
                            filteredPosts.forEach { post ->
                                fetchFeedDetail(post)
                            }
                            setLoadingMore(false)
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

    private suspend fun setLoading(isLoading: Boolean) {
        _state.emit(
            _state.value.copy(
                isLoading = isLoading,
            ),
        )
    }

    fun reportVideo(
        pageNo: Int,
        reason: VideoReportReason,
        text: String,
    ) {
        coroutineScope
            .launch {
                val currentFeed = _state.value.feedDetails[pageNo]
                setLoading(true)
                requiredUseCases.reportVideoUseCase
                    .invoke(
                        parameter =
                            ReportRequestParams(
                                postId = currentFeed.postID,
                                videoId = currentFeed.videoID,
                                reason = text.ifEmpty { reason.reason },
                                canisterID = currentFeed.canisterID,
                                principal = currentFeed.principalID,
                            ),
                    ).mapBoth(
                        success = {
                            setLoading(false)
                            toggleReportSheet(false, pageNo)
                            // Remove post from feed
                            val updatedPosts = _state.value.posts.toMutableList()
                            val updatedFeedDetails = _state.value.feedDetails.toMutableList()

                            // Find and remove the post with matching videoID
                            val postIndex =
                                updatedPosts.indexOfFirst { it.videoID == currentFeed.videoID }
                            if (postIndex != -1) {
                                updatedPosts.removeAt(postIndex)
                            }

                            // Find and remove the feed detail with matching videoID
                            val feedDetailIndex =
                                updatedFeedDetails.indexOfFirst { it.videoID == currentFeed.videoID }
                            if (feedDetailIndex != -1) {
                                updatedFeedDetails.removeAt(feedDetailIndex)
                            }

                            // Update state with modified lists
                            _state.emit(
                                _state.value.copy(
                                    posts = updatedPosts,
                                    feedDetails = updatedFeedDetails,
                                    // Adjust current page if necessary to prevent out of bounds
                                    currentPageOfFeed =
                                        minOf(
                                            pageNo,
                                            updatedFeedDetails.size - 1,
                                        ).coerceAtLeast(0),
                                ),
                            )
                        },
                        failure = {
                            setLoading(false)
                            toggleReportSheet(true, pageNo)
                        },
                    )
            }
    }

    fun toggleReportSheet(
        isOpen: Boolean,
        pageNo: Int,
    ) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    reportSheetState =
                        if (isOpen) {
                            ReportSheetState.Open(pageNo)
                        } else {
                            ReportSheetState.Closed
                        },
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
    val isPostDescriptionExpanded: Boolean = false,
    val videoData: VideoData = VideoData(),
    val isLoading: Boolean = false,
    val reportSheetState: ReportSheetState = ReportSheetState.Closed,
)

data class VideoData(
    val didLogFirstSecondWatched: Boolean = false,
    val didLogFullVideoWatched: Boolean = false,
    val lastKnownCurrentTime: Int = 0,
    val lastKnownTotalTime: Int = 0,
    val isFirstTimeUpdate: Boolean = true,
)

sealed interface ReportSheetState {
    data object Closed : ReportSheetState
    data class Open(
        val pageNo: Int,
        val reasons: List<VideoReportReason> =
            listOf(
                VideoReportReason.NUDITY_PORN,
                VideoReportReason.VIOLENCE,
                VideoReportReason.OFFENSIVE,
                VideoReportReason.SPAM,
                VideoReportReason.OTHERS,
            ),
    ) : ReportSheetState
}

enum class VideoReportReason(
    val reason: String,
) {
    NUDITY_PORN("Nudity / Porn"),
    VIOLENCE("Violence / Gore"),
    OFFENSIVE("Offensive"),
    SPAM("Spam / Ad"),
    OTHERS("Others"),
}

data class RequiredUseCases(
    val getInitialFeedUseCase: GetInitialFeedUseCase,
    val fetchMoreFeedUseCase: FetchMoreFeedUseCase,
    val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
    val reportVideoUseCase: ReportVideoUseCase,
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
