package com.yral.shared.features.feed.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.FeedFeatureFlags
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.shared.analytics.events.CtaType
import com.yral.shared.analytics.events.FeedType
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.utils.processFirstNSuspendFlow
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.data.domain.useCases.GetVideoViewsUseCase
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.feed.analytics.FeedTelemetry
import com.yral.shared.features.feed.domain.useCases.CheckVideoVoteUseCase
import com.yral.shared.features.feed.domain.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.domain.useCases.FetchFeedDetailsWithCreatorInfoUseCase
import com.yral.shared.features.feed.domain.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.domain.useCases.GetAIFeedUseCase
import com.yral.shared.features.feed.domain.useCases.GetInitialFeedUseCase
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.routes.api.PendingAppRouteStore
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.LinkInput
import com.yral.shared.libs.sharing.ShareService
import com.yral.shared.reportVideo.domain.ReportRequestParams
import com.yral.shared.reportVideo.domain.ReportVideoUseCase
import com.yral.shared.reportVideo.domain.models.ReportSheetState
import com.yral.shared.reportVideo.domain.models.ReportVideoData
import com.yral.shared.rust.service.domain.usecases.FollowUserParams
import com.yral.shared.rust.service.domain.usecases.FollowUserUseCase
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LongParameterList", "LargeClass")
class FeedViewModel(
    appDispatchers: AppDispatchers,
    private val sessionManager: SessionManager,
    private val requiredUseCases: RequiredUseCases,
    private val crashlyticsManager: CrashlyticsManager,
    private val feedTelemetry: FeedTelemetry,
    authClientFactory: AuthClientFactory,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val flagManager: FeatureFlagManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                toggleSignupFailed(true)
            }

    companion object {
        const val PRE_FETCH_BEFORE_LAST = 5
        private const val FIRST_SECOND_WATCHED_THRESHOLD_MS = 100L
        private val ANALYTICS_VIDEO_STARTED_RANGE = 0L..1000L
        private val ANALYTICS_VIDEO_VIEWED_RANGE = 3000L..4000L
        private const val FULL_VIDEO_WATCHED_THRESHOLD = 95.0
        private const val MAX_PAGE_SIZE = 100
        private const val MAX_PAGE_SIZE_AI_FEED = 50
        private const val FEEDS_PAGE_SIZE = 10
        private const val FEEDS_PAGE_SIZE_AI_FEED = 10
        private const val SUFFICIENT_NEW_REQUIRED = 10
        const val SIGN_UP_PAGE = 9
        private const val PAGER_STATE_REFRESH_BUFFER_MS = 100L
        const val FOLLOW_NUDGE_PAGE = 5
    }

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val feedEventsChannel = Channel<FeedEvents>(Channel.CONFLATED)
    val feedEvents = feedEventsChannel.receiveAsFlow()

    init {
        initAvailableFeeds()
        when (_state.value.feedType) {
            FeedType.AI -> {
                fetchAIFeed(
                    currentBatchSize = FEEDS_PAGE_SIZE_AI_FEED,
                    totalNotVotedCount = 0,
                    recursionDepth = 0,
                )
            }
            else -> initialFeedData()
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionProperty { it.followedPrincipals to it.unFollowedPrincipals }
                .collect { (followedPrincipals, unfollowedPrincipals) ->
                    _state.update {
                        it.copy(
                            feedDetails =
                                it.feedDetails.map { details ->
                                    when (details.principalID) {
                                        in followedPrincipals -> details.copy(isFollowing = true)
                                        in unfollowedPrincipals -> details.copy(isFollowing = false)
                                        else -> details
                                    }
                                },
                        )
                    }
                }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn -> _state.update { it.copy(isLoggedIn = isSocialSignIn) } }
        }
    }

    private fun initAvailableFeeds() {
        val availableFeedTypes =
            flagManager
                .get(FeedFeatureFlags.FeedTypes.AvailableTypes)
                .split(",")
                .mapNotNull { name -> FeedType.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } }
        val selectedType = availableFeedTypes.firstOrNull() ?: FeedType.DEFAULT
        _state.update {
            it.copy(
                availableFeedTypes = availableFeedTypes,
                feedType = selectedType,
            )
        }
    }

    private fun initialFeedData() {
        coroutineScope.launch {
            sessionManager.userPrincipal?.let { userId ->
                setLoadingMore(true)
                requiredUseCases.getInitialFeedUseCase
                    .invoke(GetInitialFeedUseCase.Params(userId = userId))
                    .onSuccess { result ->
                        val posts = result.posts
                        Logger.d("FeedPagination") { "posts in initialFeed ${posts.size}" }
                        if (posts.isEmpty()) {
                            setLoadingMore(false)
                            loadMoreFeed()
                        } else {
                            crashlyticsManager.recordException(
                                YralException("Initial cache feed empty"),
                                ExceptionType.FEED,
                            )
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
    }

    private fun fetchAIFeed(
        currentBatchSize: Int,
        totalNotVotedCount: Int,
        recursionDepth: Int = 0,
        maxDepth: Int = MAX_PAGE_SIZE_AI_FEED / FEEDS_PAGE_SIZE_AI_FEED,
    ) {
        if (recursionDepth >= maxDepth) {
            setLoadingMore(false)
            // Safeguard: Max recursion depth reached
            // Optionally log or notify here
            return
        }
        coroutineScope.launch {
            sessionManager.userPrincipal?.let { userPrincipal ->
                setLoadingMore(true)
                requiredUseCases.getAIFeedUseCase
                    .invoke(
                        parameter = GetAIFeedUseCase.Params(userId = userPrincipal, batchSize = currentBatchSize),
                    ).onSuccess { result ->
                        val posts = result.posts
                        Logger.d("FeedPagination") { "posts in ai feed ${posts.size}" }
                        if (posts.isEmpty()) {
                            setLoadingMore(false)
                            updateFeedType(FeedType.DEFAULT)
                        } else {
                            val notVotedCount = filterVotedAndFetchDetails(posts)
                            val newTotal = totalNotVotedCount + notVotedCount
                            Logger.d("FeedPagination") { "notVotedCount in ai feed $notVotedCount" }
                            if (notVotedCount < SUFFICIENT_NEW_REQUIRED) {
                                val nextBatchSize =
                                    (currentBatchSize + FEEDS_PAGE_SIZE_AI_FEED).coerceAtMost(MAX_PAGE_SIZE_AI_FEED)
                                fetchAIFeed(
                                    currentBatchSize = nextBatchSize,
                                    totalNotVotedCount = newTotal,
                                    recursionDepth = recursionDepth + 1,
                                )
                            } else {
                                setLoadingMore(false)
                            }
                        }
                    }.onFailure {
                        setLoadingMore(false)
                        Logger.e("FeedPagination") { "Error fetching ai feed $it" }
                    }
            }
        }
    }

    /**
     * Call this when app is opened via a deeplink to a specific video.
     * The video identified by [postId] and [canisterId] will be fetched and
     * shown first to the user (prepended to the existing feed), followed by the normal feed.
     * This method is idempotent per ViewModel instance.
     */
    fun showDeeplinkedVideoFirst(
        postId: String,
        canisterId: String,
        publisherUserId: String,
    ) {
        Logger.d("LinkSharing") { "Fetching details for $postId $canisterId $publisherUserId" }
        coroutineScope.launch {
            // If details already exist, move to front and return; else fetch and show.
            if (tryShowExistingDeeplink(postId, canisterId, publisherUserId)) return@launch
            fetchAndShowDeeplink(postId, canisterId, publisherUserId)
        }
    }

    private suspend fun tryShowExistingDeeplink(
        postId: String,
        canisterId: String,
        publisherUserId: String,
    ): Boolean {
        // currently setting currentPageOfFeed to 0 does not reset the position and requires
        // changes to pager state handling. Setting fetching to true kinda recreates the Pager
        setDeeplinkFetching(true)
        delay(PAGER_STATE_REFRESH_BUFFER_MS)

        val existingDetail =
            _state.value.feedDetails.firstOrNull {
                it.postID == postId && it.canisterID == canisterId && it.principalID == publisherUserId
            }
        if (existingDetail != null) {
            _state.update { currentState ->
                Logger.d("LinkSharing") { "Existing detail found $existingDetail" }
                addDeeplinkData(currentState, existingDetail)
            }
            return true
        }
        setDeeplinkFetching(true)
        return false
    }

    private suspend fun fetchAndShowDeeplink(
        postId: String,
        canisterId: String,
        publisherUserId: String,
    ) {
        // We only need canisterId, publisherUserId and postId for fetching details.
        // Other Post fields are not used by the underlying data source for this call.
        val post =
            Post(
                canisterID = canisterId,
                publisherUserId = publisherUserId,
                postID = postId,
                videoID = "",
                nsfwProbability = null,
                numViewsAll = null,
                numViewsLoggedIn = null,
            )

        if (publisherUserId.isNotBlank()) {
            setDeeplinkFetching(true)
            Logger.d("LinkSharing") { "Fetching details with creator Info" }
            requiredUseCases
                .fetchVideoDetailsWithCreatorInfoUseCase(post)
                .onSuccess { detail ->
                    Logger.d("LinkSharing") { "Details Received $detail" }
                    detail?.let {
                        Logger.d("LinkSharing") { "Fetching views for ${detail.videoID} current: ${detail.viewCount}" }
                        requiredUseCases
                            .videoViewsUseCase(
                                parameter = GetVideoViewsUseCase.Params(listOf(detail.videoID)),
                            ).onSuccess { views ->
                                feedTelemetry.onDeeplink(detail.videoID)
                                views.firstOrNull()?.allViews?.let { allViews ->
                                    Logger.d("LinkSharing") { "View count : $allViews" }
                                    _state.update { currentState ->
                                        addDeeplinkData(currentState, detail.copy(viewCount = allViews))
                                    }
                                } ?: run {
                                    Logger.d("LinkSharing") { "Failed to fetch view count" }
                                    _state.update { currentState -> addDeeplinkData(currentState, detail) }
                                }
                            }.onFailure {
                                Logger.d("LinkSharing") { "Failed to fetch view count" }
                                feedTelemetry.onDeeplink(detail.videoID)
                                _state.update { currentState -> addDeeplinkData(currentState, detail) }
                            }
                    } ?: run {
                        val exceptionMessage = "Detail is null for $postId in deeplink"
                        crashlyticsManager.recordException(
                            YralException(exceptionMessage),
                            ExceptionType.FEED,
                        )
                        setDeeplinkFetching(false)
                        Logger.e("LinkSharing") { exceptionMessage }
                    }
                }.onFailure { throwable ->
                    Logger.e("LinkSharing", throwable) {
                        "Failed to fetch deep linked video details for postId=$postId canisterId=$canisterId"
                    }
                    setDeeplinkFetching(false)
                }
        } else {
            setDeeplinkFetching(false)
        }
    }

    private fun addDeeplinkData(
        currentState: FeedState,
        details: FeedDetails,
    ): FeedState {
        // Remove existing occurrence and insert at current page index
        val filtered = currentState.feedDetails.filterNot { it.videoID == details.videoID }
        val targetIndex = currentState.currentPageOfFeed.coerceIn(0, filtered.size)
        Logger.d("LinkSharing") { "targetIndex $targetIndex" }
        val updatedFeedDetails =
            filtered
                .toMutableList()
                .apply {
                    add(targetIndex, details)
                }.toList()
        return currentState.copy(
            feedDetails = updatedFeedDetails,
            currentPageOfFeed = targetIndex,
            videoData = VideoData(),
            isDeeplinkFetching = false,
        )
    }

    @Suppress("LongMethod")
    private suspend fun filterVotedAndFetchDetails(
        posts: List<Post>,
        checkVotes: Boolean = false,
    ): Int {
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
                    requiredUseCases.fetchVideoDetailsWithCreatorInfoUseCase
                        .invoke(post)
                        .map { detail ->
                            if (detail == null) {
                                // Logger.e("FeedPagination") { "Detail is null for ${post.postID}" }
                                crashlyticsManager.recordException(
                                    YralException("Detail is null for ${post.postID}"),
                                    ExceptionType.FEED,
                                )
                            }
                            Logger.e("FeedPagination") {
                                "${post.videoID} view count " +
                                    "in post ${post.numViewsAll} " +
                                    "in feed details ${detail?.viewCount}"
                            }
                            val updatedDetail =
                                post.numViewsAll?.let { views -> detail?.copy(viewCount = views) } ?: detail
                            updatedDetail to
                                if (checkVotes) {
                                    detail?.let { isAlreadyVoted(it) }
                                } else {
                                    false
                                }
                        }
                },
            ).collect { result ->
                result
                    .onSuccess { (detail, isVoted) ->
                        val existingDetailIds =
                            _state.value.feedDetails.mapTo(HashSet()) { it.videoID }
                        detail?.let {
                            if (detail.videoID !in existingDetailIds) {
                                if (isVoted == false) {
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
                        } ?: _state.update { it.copy(pendingFetchDetails = it.pendingFetchDetails - 1) }
                    }.onFailure {
                        Logger.e("FeedPagination") { "Failed to fetch details $it" }
                        _state.update { state -> state.copy(pendingFetchDetails = state.pendingFetchDetails - 1) }
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
            if (_state.value.feedType == FeedType.AI) {
                fetchAIFeed(
                    currentBatchSize = FEEDS_PAGE_SIZE_AI_FEED,
                    totalNotVotedCount = 0,
                    recursionDepth = 0,
                )
            } else {
                loadMoreFeedRecursively(
                    currentBatchSize = FEEDS_PAGE_SIZE,
                    totalNotVotedCount = 0,
                    recursionDepth = 0,
                )
            }
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
                        if (totalNotVotedCount < SUFFICIENT_NEW_REQUIRED) {
                            crashlyticsManager.recordException(
                                YralException("Feed ran dry"),
                                ExceptionType.FEED,
                            )
                        }
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
        refreshVideoViewCount(currentState.feedDetails[currentState.currentPageOfFeed])
    }

    private fun refreshVideoViewCount(detail: FeedDetails) {
        viewModelScope.launch {
            requiredUseCases
                .videoViewsUseCase(
                    parameter = GetVideoViewsUseCase.Params(listOf(detail.videoID)),
                ).onSuccess { views ->
                    views.firstOrNull()?.allViews?.let { allViews ->
                        Logger.d("ViewCount") { "View count : $allViews" }
                        _state.update { currentState ->
                            val list = currentState.feedDetails
                            val index = list.indexOfFirst { it.videoID == detail.videoID }
                            if (index == -1) {
                                currentState
                            } else {
                                currentState.copy(
                                    feedDetails =
                                        list
                                            .toMutableList()
                                            .apply { this[index] = this[index].copy(viewCount = allViews) },
                                )
                            }
                        }
                    }
                }.onFailure {
                    Logger.d("ViewCount") { "Failed to fetch view count" }
                }
        }
    }

    fun setPostDescriptionExpanded(isExpanded: Boolean) {
        _state.update { it.copy(isPostDescriptionExpanded = isExpanded) }
    }

    fun recordTime(
        currentTime: Int,
        totalTime: Int,
    ) {
        coroutineScope.launch {
            // Get current state values before any updates
            val videoData = _state.value.videoData
            val currentFeedDetails = _state.value.feedDetails[_state.value.currentPageOfFeed]
            when (currentTime) {
                in ANALYTICS_VIDEO_STARTED_RANGE -> feedTelemetry.trackVideoStarted(currentFeedDetails)

                in ANALYTICS_VIDEO_VIEWED_RANGE -> {
                    feedTelemetry.trackVideoViewed(currentFeedDetails)
                    feedTelemetry.resetVideoStarted(currentFeedDetails.videoID)
                }
            }

            // If we've already logged full video watched, no need to continue processing
            if (videoData.didLogFullVideoWatched) {
                return@launch
            }

            // Check for threshold crossing - only if not the first update and we haven't logged it yet
            val shouldLogFirstSecond =
                !videoData.isFirstTimeUpdate &&
                    !videoData.didLogFirstSecondWatched &&
                    currentTime >= FIRST_SECOND_WATCHED_THRESHOLD_MS
            val shouldLogFullVideo =
                currentTime.percentageOf(totalTime) >= FULL_VIDEO_WATCHED_THRESHOLD &&
                    !videoData.didLogFullVideoWatched
            val shouldLog3Second =
                !videoData.isFirstTimeUpdate &&
                    currentTime in ANALYTICS_VIDEO_VIEWED_RANGE &&
                    !videoData.didLog3SecondWatched
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
                recordEvent(videoData = _state.value.videoData.copy(didLogFirstSecondWatched = true))
            } else if (shouldLog3Second) {
                recordEvent(videoData = _state.value.videoData.copy(didLog3SecondWatched = true))
            } else if (shouldLogFullVideo) {
                recordEvent(videoData = state.value.videoData.copy(didLogFullVideoWatched = true))
            }
        }
    }

    private fun recordEvent(videoData: VideoData) {
        val currentFeed = _state.value.feedDetails[_state.value.currentPageOfFeed]
        feedTelemetry.onVideoDurationWatched(
            feedDetails = currentFeed,
            isLoggedIn = _state.value.isLoggedIn,
            currentTime = videoData.lastKnownCurrentTime,
            totalTime = videoData.lastKnownTotalTime,
        )
        _state.update { it.copy(videoData = videoData) }
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

    private fun setReporting(isReporting: Boolean) {
        _state.update { it.copy(isReporting = isReporting) }
    }

    private fun setDeeplinkFetching(isFetching: Boolean) {
        _state.update { it.copy(isDeeplinkFetching = isFetching) }
    }

    fun reportVideo(
        pageNo: Int,
        reportVideoData: ReportVideoData,
    ) {
        coroutineScope.launch {
            val currentFeed = _state.value.feedDetails[pageNo]
            setReporting(true)
            requiredUseCases.reportVideoUseCase
                .invoke(
                    parameter =
                        ReportRequestParams(
                            postId = currentFeed.postID,
                            videoId = currentFeed.videoID,
                            reason = reportVideoData.otherReasonText.ifEmpty { reportVideoData.reason.reason },
                            canisterID = currentFeed.canisterID,
                            principal = currentFeed.principalID,
                        ),
                ).onSuccess {
                    setReporting(false)
                    with(reportVideoData) {
                        ToastManager.showToast(
                            type = ToastType.Big(successMessage.first, successMessage.second),
                            status = ToastStatus.Success,
                        )
                    }
                    feedTelemetry.videoReportedSuccessfully(currentFeed, reportVideoData.reason)
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
                    setReporting(false)
                    toggleReportSheet(true, pageNo)
                }
        }
    }

    fun toggleReportSheet(
        isOpen: Boolean,
        pageNo: Int,
    ) {
        val currentDetail = _state.value.feedDetails[_state.value.currentPageOfFeed]
        var sheetState: ReportSheetState = ReportSheetState.Closed
        if (isOpen) {
            feedTelemetry.videoClicked(
                feedDetails = currentDetail,
                ctaType = CtaType.REPORT,
            )
            sheetState = ReportSheetState.Open(pageNo)
        }
        _state.update { it.copy(reportSheetState = sheetState) }
    }

    fun onShareClicked(
        feedDetails: FeedDetails,
        message: String,
        description: String,
    ) {
        feedTelemetry.onShareClicked(feedDetails, sessionManager.userPrincipal)
        viewModelScope.launch {
            // Build internal deep link using UrlBuilder and PostDetailsRoute
            val route =
                PostDetailsRoute(
                    canisterId = feedDetails.canisterID,
                    postId = feedDetails.postID,
                    publisherUserId = feedDetails.principalID,
                )
            val internalUrl = urlBuilder.build(route) ?: feedDetails.url
            runSuspendCatching {
                val link =
                    linkGenerator.generateShareLink(
                        LinkInput(
                            internalUrl = internalUrl,
                            title = message,
                            description = description,
                            feature = "share",
                            tags = listOf("organic", "user_share"),
                            contentImageUrl = feedDetails.thumbnail,
                        ),
                    )
                val text = "$message $link"
                shareService.shareImageWithText(
                    imageUrl = feedDetails.thumbnail,
                    text = text,
                )
            }.onFailure {
                Logger.e(FeedViewModel::class.simpleName!!, it) { "Failed to share post" }
                crashlyticsManager.recordException(
                    YralException(it),
                    ExceptionType.DEEPLINK,
                )
            }
        }
    }

    fun registerTrace(
        videoID: String,
        traceType: String,
    ) {
        _state.update { it.copy(videoTracing = it.videoTracing + (videoID to traceType)) }
    }

    fun isAlreadyTraced(
        videoID: String,
        traceType: String,
    ): Boolean =
        _state.value.videoTracing.any {
            videoID == it.first && traceType == it.second
        }

    @Suppress("TooGenericExceptionCaught")
    fun signInWithSocial(
        context: Any,
        provider: SocialProvider,
    ) {
        coroutineScope.launch {
            try {
                authClient.signInWithSocial(context, provider)
            } catch (e: Exception) {
                crashlyticsManager.recordException(e, ExceptionType.AUTH)
                toggleSignupFailed(true)
            }
        }
    }

    fun toggleSignupFailed(shouldShow: Boolean) {
        _state.update { it.copy(showSignupFailedSheet = shouldShow) }
    }

    fun pushScreenView() {
        feedTelemetry.onFeedPageViewed()
    }

    fun getTncLink(): String = flagManager.get(AccountFeatureFlags.AccountLinks.Links).tnc

    fun updateFeedType(feedType: FeedType) {
        if (_state.value.isLoadingMore || _state.value.feedType == feedType) return
        _state.update {
            val updatedFeed = it.feedDetails.take(it.currentPageOfFeed + 1)
            val updatedVideoIds = updatedFeed.mapTo(HashSet()) { feed -> feed.videoID }
            it.copy(
                feedType = feedType,
                feedDetails = updatedFeed,
                isLoadingMore = false,
                posts = it.posts.filter { post -> post.videoID !in updatedVideoIds },
            )
        }
    }

    fun pushFeedToggleClicked(
        feedType: FeedType,
        isExpanded: Boolean,
    ) {
        feedTelemetry.feedToggleClicked(feedType, isExpanded)
    }

    fun follow(canisterData: CanisterData) {
        if (_state.value.isFollowInProgress) return
        pushFollowClicked(canisterData.userPrincipalId)
        coroutineScope.launch {
            sessionManager.userPrincipal?.let { userPrincipal ->
                _state.update { it.copy(isFollowInProgress = true) }
                requiredUseCases
                    .followUserUseCase(
                        parameter =
                            FollowUserParams(
                                principal = userPrincipal,
                                targetPrincipal = canisterData.userPrincipalId,
                            ),
                    ).onSuccess {
                        _state.update { it.copy(isFollowInProgress = false) }
                        feedEventsChannel.trySend(FeedEvents.FollowedSuccessfully(canisterData.username ?: ""))
                        sessionManager.addPrincipalToFollow(canisterData.userPrincipalId)
                        Logger.d("Follow") { "Started following" }
                    }.onFailure { e ->
                        _state.update { it.copy(isFollowInProgress = false) }
                        feedEventsChannel.trySend(FeedEvents.Failed(e.message ?: "Follow failed"))
                        Logger.d("Follow") { "Follow request failed $e" }
                    }
            }
        }
    }

    fun consumePendingSharedVideoRouteIfNeeded() {
        sessionManager.userPrincipal?.let {
            val route = PendingAppRouteStore.peek() as? PostDetailsRoute
            if (route != null && _state.value.isLoggedIn) {
                Logger.d("LinkSharing") { "Found pending deeplink route $route ${_state.value.feedDetails[0].videoID}" }
                PendingAppRouteStore.consume()
                showDeeplinkedVideoFirst(
                    postId = route.postId,
                    canisterId = route.canisterId,
                    publisherUserId = route.publisherUserId,
                )
            }
        }
    }

    fun pushFollowClicked(publisherUserId: String) {
        feedTelemetry.followClicked(publisherUserId)
    }

    data class RequiredUseCases(
        val getInitialFeedUseCase: GetInitialFeedUseCase,
        val fetchMoreFeedUseCase: FetchMoreFeedUseCase,
        val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
        val fetchVideoDetailsWithCreatorInfoUseCase: FetchFeedDetailsWithCreatorInfoUseCase,
        val reportVideoUseCase: ReportVideoUseCase,
        val checkVideoVoteUseCase: CheckVideoVoteUseCase,
        val getAIFeedUseCase: GetAIFeedUseCase,
        val followUserUseCase: FollowUserUseCase,
        val videoViewsUseCase: GetVideoViewsUseCase,
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
    val isReporting: Boolean = false,
    val isDeeplinkFetching: Boolean = false,
    val reportSheetState: ReportSheetState = ReportSheetState.Closed,
    val showSignupFailedSheet: Boolean = false,
    val overlayType: OverlayType = OverlayType.DAILY_RANK,
    val isLoggedIn: Boolean = false,
    val availableFeedTypes: List<FeedType> = listOf(FeedType.DEFAULT),
    val feedType: FeedType = FeedType.DEFAULT,
    val isFollowInProgress: Boolean = false,
)

enum class OverlayType {
    DEFAULT,
    GAME_TOGGLE,
    DAILY_RANK,
}

data class VideoData(
    val didLog3SecondWatched: Boolean = false,
    val didLogFirstSecondWatched: Boolean = false,
    val didLogFullVideoWatched: Boolean = false,
    val lastKnownCurrentTime: Int = 0,
    val lastKnownTotalTime: Int = 0,
    val isFirstTimeUpdate: Boolean = true,
)

sealed class FeedEvents {
    data class FollowedSuccessfully(
        val userName: String,
    ) : FeedEvents()
    data object UnfollowedSuccessfully : FeedEvents()
    data class Failed(
        val message: String,
    ) : FeedEvents()
}

@Suppress("MagicNumber")
internal fun Int.percentageOf(total: Int): Double =
    if (total > 0) {
        (this.toDouble() / total.toDouble()) * 100.0
    } else {
        0.0
    }
