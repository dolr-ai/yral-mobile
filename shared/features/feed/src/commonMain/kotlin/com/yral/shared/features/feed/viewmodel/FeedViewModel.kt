package com.yral.shared.features.feed.viewmodel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.events.CtaType
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.utils.processFirstNSuspendFlow
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.feed.analytics.FeedTelemetry
import com.yral.shared.features.feed.useCases.CheckVideoVoteUseCase
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.features.feed.useCases.ReportRequestParams
import com.yral.shared.features.feed.useCases.ReportVideoUseCase
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

@Suppress("TooManyFunctions")
class FeedViewModel(
    appDispatchers: AppDispatchers,
    private val sessionManager: SessionManager,
    private val requiredUseCases: RequiredUseCases,
    private val crashlyticsManager: CrashlyticsManager,
    private val feedTelemetry: FeedTelemetry,
    authClientFactory: AuthClientFactory,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                toggleSignupFailed(true)
            }

    companion object {
        const val PRE_FETCH_BEFORE_LAST = 5
        private const val FIRST_SECOND_WATCHED_THRESHOLD_MS = 1000L
        private val ANALYTICS_VIDEO_STARTED_RANGE = 0L..1000L
        private val ANALYTICS_VIDEO_VIEWED_RANGE = 3000L..4000L
        private const val FULL_VIDEO_WATCHED_THRESHOLD = 95.0
        private const val MAX_PAGE_SIZE = 100
        private const val FEEDS_PAGE_SIZE = 10
        private const val SUFFICIENT_NEW_REQUIRED = 10
        const val SIGN_UP_PAGE = 9
    }

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        coroutineScope.launch { initialFeedData() }
    }

    private suspend fun initialFeedData() {
        sessionManager.userPrincipal?.let { userId ->
            setLoadingMore(true)
            requiredUseCases.getInitialFeedUseCase
                .invoke(
                    parameter =
                        GetInitialFeedUseCase.Params(
                            userId = userId,
                            filterResults = emptyList(),
                        ),
                ).onSuccess { result ->
                    val posts = result.posts
                    Logger.d("FeedPagination") { "posts in initialFeed ${posts.size}" }
                    if (posts.isEmpty()) {
                        setLoadingMore(false)
                        loadMoreFeed()
                    } else {
                        val notVotedCount = filterVotedAndFetchDetails(posts)
                        Logger.d("FeedPagination") { "notVotedCount in initialFeed $notVotedCount" }
                        if (notVotedCount < SUFFICIENT_NEW_REQUIRED) {
                            setLoadingMore(false)
                            loadMoreFeed()
                        } else {
                            setLoadingMore(false)
                        }
                    }
                }.onFailure {
                    setLoadingMore(false)
                    loadMoreFeed()
                }
        }
    }

    private suspend fun filterVotedAndFetchDetails(posts: List<Post>): Int {
        val fetchedIds = _state.value.posts.mapTo(HashSet()) { it.videoID }
        val newPosts = posts.filter { post -> post.videoID !in fetchedIds }
        _state.update { it.copy(posts = it.posts + newPosts) }
        Logger.d("FeedPagination") { "no of duplicate posts ${posts.size - newPosts.size}" }
        val count = MutableStateFlow(0)
        newPosts
            .processFirstNSuspendFlow(
                n = newPosts.size,
                process = { post ->
                    _state.update { it.copy(pendingFetchDetails = it.pendingFetchDetails + 1) }
                    requiredUseCases.fetchFeedDetailsUseCase.invoke(post)
                },
            ).collect { detail ->
                detail
                    .onSuccess { detail ->
                        val existingDetailIds =
                            _state.value.feedDetails.mapTo(HashSet()) { it.videoID }
                        if (detail.videoID !in existingDetailIds) {
                            val isVoted = isAlreadyVoted(detail)
                            Logger.d("FeedPagination") { "isVoted $isVoted video Id: ${detail.videoID}" }
                            if (!isVoted) {
                                count.update { it + 1 }
                                _state.update { currentState ->
                                    // Check if this feedDetail already exists to prevent duplicates
                                    val existingDetailIds =
                                        currentState.feedDetails.mapTo(HashSet()) { it.videoID }
                                    val updatedFeedDetails =
                                        if (detail.videoID !in existingDetailIds) {
                                            currentState.feedDetails + detail
                                        } else {
                                            currentState.feedDetails
                                        }
                                    currentState.copy(
                                        feedDetails = updatedFeedDetails,
                                        pendingFetchDetails = currentState.pendingFetchDetails - 1,
                                    )
                                }
                            } else {
                                _state.update { it.copy(pendingFetchDetails = it.pendingFetchDetails - 1) }
                            }
                        } else {
                            _state.update { it.copy(pendingFetchDetails = it.pendingFetchDetails - 1) }
                        }
                    }.onFailure {
                        _state.update { it.copy(pendingFetchDetails = it.pendingFetchDetails - 1) }
                    }
            }
        return count.value
    }

    private suspend fun isAlreadyVoted(detail: FeedDetails): Boolean =
        sessionManager.userPrincipal?.let { userPrincipal ->
            requiredUseCases.checkVideoVoteUseCase
                .invoke(
                    CheckVideoVoteUseCase.Params(
                        videoId = detail.videoID,
                        principalId = userPrincipal,
                    ),
                ).value
        } ?: false

    fun loadMoreFeed() {
        if (_state.value.isLoadingMore) return
        coroutineScope.launch {
            loadMoreFeedRecursively(
                currentBatchSize = FEEDS_PAGE_SIZE,
                totalNotVotedCount = 0,
                recursionDepth = 0,
            )
        }
    }

    private suspend fun loadMoreFeedRecursively(
        currentBatchSize: Int,
        totalNotVotedCount: Int,
        recursionDepth: Int = 0,
        maxDepth: Int = MAX_PAGE_SIZE / FEEDS_PAGE_SIZE,
    ) {
        if (recursionDepth >= maxDepth) {
            setLoadingMore(false)
            // Safeguard: Max recursion depth reached
            // Optionally log or notify here
            return
        }
        sessionManager.userPrincipal?.let { userId ->
            setLoadingMore(true)
            requiredUseCases.fetchMoreFeedUseCase
                .invoke(
                    parameter =
                        FetchMoreFeedUseCase.Params(
                            userId = userId,
                            filterResults =
                                _state.value.posts.map { post ->
                                    post.toFilteredResult()
                                },
                            batchSize = currentBatchSize.coerceAtMost(MAX_PAGE_SIZE),
                        ),
                ).onSuccess { moreFeed ->
                    val posts = moreFeed.posts
                    Logger.d("FeedPagination") { "posts in loadMoreFeed ${posts.size}" }
                    if (posts.isNotEmpty()) {
                        val notVotedCount = filterVotedAndFetchDetails(posts)
                        val newTotal = totalNotVotedCount + notVotedCount
                        Logger.d("FeedPagination") { "notVotedCount in loadMoreFeed $newTotal" }
                        if (newTotal < SUFFICIENT_NEW_REQUIRED) {
                            val nextBatchSize =
                                (currentBatchSize + FEEDS_PAGE_SIZE).coerceAtMost(MAX_PAGE_SIZE)
                            loadMoreFeedRecursively(
                                currentBatchSize = nextBatchSize,
                                totalNotVotedCount = newTotal,
                                recursionDepth = recursionDepth + 1,
                            )
                        } else {
                            setLoadingMore(false)
                        }
                    } else {
                        setLoadingMore(false)
                    }
                }.onFailure {
                    setLoadingMore(false)
                }
        }
    }

    private fun setLoadingMore(isLoading: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                isLoadingMore = isLoading,
            )
        }
    }

    fun onCurrentPageChange(pageNo: Int) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    currentPageOfFeed = pageNo,
                    videoData = VideoData(), // Reset all video data for new page
                )
            }
            val currentState = _state.value
            if (currentState.currentPageOfFeed < currentState.feedDetails.size) {
                feedTelemetry.trackVideoImpression(
                    feedDetails = currentState.feedDetails[currentState.currentPageOfFeed],
                )
            }
        }
    }

    fun setPostDescriptionExpanded(isExpanded: Boolean) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    isPostDescriptionExpanded = isExpanded,
                )
            }
        }
    }

    fun recordTime(
        currentTime: Int,
        totalTime: Int,
    ) {
        coroutineScope.launch {
            val currentFeedDetails = _state.value.feedDetails[_state.value.currentPageOfFeed]
            when (currentTime) {
                in ANALYTICS_VIDEO_STARTED_RANGE -> feedTelemetry.trackVideoStarted(currentFeedDetails)
                in ANALYTICS_VIDEO_VIEWED_RANGE -> {
                    feedTelemetry.trackVideoViewed(currentFeedDetails)
                    feedTelemetry.resetVideoStarted(currentFeedDetails.videoID)
                }
            }

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
            _state.update {
                it.copy(
                    videoData =
                        it.videoData.copy(
                            lastKnownCurrentTime = currentTime,
                            lastKnownTotalTime = totalTime,
                            isFirstTimeUpdate = false, // Mark that we've had at least one update
                        ),
                )
            }
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

    private suspend fun recordEvent(videoData: VideoData) {
        val currentFeed = _state.value.feedDetails[_state.value.currentPageOfFeed]
        feedTelemetry.onVideoDurationWatched(
            feedDetails = currentFeed,
            isLoggedIn = isLoggedIn(),
            currentTime = videoData.lastKnownCurrentTime,
            totalTime = videoData.lastKnownTotalTime,
        )
        _state.update { currentState ->
            currentState.copy(
                videoData = videoData,
            )
        }
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

    private fun setLoading(isLoading: Boolean) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    isLoading = isLoading,
                )
            }
        }
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
                    ).onSuccess {
                        setLoading(false)
                        feedTelemetry.videoReportedSuccessfully(currentFeed, reason)
                        toggleReportSheet(false, pageNo)
                        _state.update { currentState ->
                            val updatedFeedDetails = currentState.feedDetails.toMutableList()

                            // Find and remove the feed detail with matching videoID
                            val feedDetailIndex =
                                updatedFeedDetails.indexOfFirst { it.videoID == currentFeed.videoID }
                            if (feedDetailIndex != -1) {
                                updatedFeedDetails.removeAt(feedDetailIndex)
                            }

                            currentState.copy(
                                feedDetails = updatedFeedDetails,
                                // Adjust current page if necessary to prevent out of bounds
                                currentPageOfFeed =
                                    minOf(
                                        pageNo,
                                        updatedFeedDetails.size - 1,
                                    ).coerceAtLeast(0),
                            )
                        }
                    }.onFailure {
                        setLoading(false)
                        toggleReportSheet(true, pageNo)
                    }
            }
    }

    fun toggleReportSheet(
        isOpen: Boolean,
        pageNo: Int,
    ) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    reportSheetState =
                        if (isOpen) {
                            feedTelemetry.videoClicked(
                                feedDetails = _state.value.feedDetails[_state.value.currentPageOfFeed],
                                ctaType = CtaType.REPORT,
                            )
                            ReportSheetState.Open(pageNo)
                        } else {
                            ReportSheetState.Closed
                        },
                )
            }
        }
    }

    fun registerTrace(
        videoID: String,
        traceType: String,
    ) {
        coroutineScope.launch {
            _state.update {
                it.copy(
                    videoTracing = it.videoTracing + (videoID to traceType),
                )
            }
        }
    }

    fun isAlreadyTraced(
        videoID: String,
        traceType: String,
    ): Boolean =
        _state.value.videoTracing.any {
            videoID == it.first && traceType == it.second
        }

    fun shouldMarkAnimationAsCompleted(page: Int): Boolean {
        val currentPage = _state.value.currentPageOfFeed
        return page != currentPage && currentPage < _state.value.feedDetails.size
    }

    @Suppress("TooGenericExceptionCaught")
    fun signInWithGoogle() {
        coroutineScope
            .launch {
                try {
                    authClient.signInWithSocial(SocialProvider.GOOGLE)
                } catch (e: Exception) {
                    crashlyticsManager.recordException(e)
                    toggleSignupFailed(true)
                }
            }
    }

    fun toggleSignupFailed(shouldShow: Boolean) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    showSignupFailedSheet = shouldShow,
                )
            }
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.isSocialSignIn()

    fun pushScreenView() {
        feedTelemetry.onFeedPageViewed()
    }

    data class RequiredUseCases(
        val getInitialFeedUseCase: GetInitialFeedUseCase,
        val fetchMoreFeedUseCase: FetchMoreFeedUseCase,
        val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
        val reportVideoUseCase: ReportVideoUseCase,
        val checkVideoVoteUseCase: CheckVideoVoteUseCase,
    )
}

data class FeedState(
    val posts: List<Post> = emptyList(),
    val feedDetails: List<FeedDetails> = emptyList(),
    val currentPageOfFeed: Int = 0,
    val isLoadingMore: Boolean = false,
    val pendingFetchDetails: Int = 0,
    val isPostDescriptionExpanded: Boolean = false,
    val videoData: VideoData = VideoData(),
    val videoTracing: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val reportSheetState: ReportSheetState = ReportSheetState.Closed,
    val showSignupFailedSheet: Boolean = false,
    val overlayType: OverlayType = OverlayType.DEFAULT,
)

enum class OverlayType {
    DEFAULT,
    GAME_TOGGLE,
}

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

@Suppress("MagicNumber")
internal fun Int.percentageOf(total: Int): Double =
    if (total > 0) {
        (this.toDouble() / total.toDouble()) * 100.0
    } else {
        0.0
    }
