package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.shared.analytics.events.CtaType
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.utils.getAccountInfo
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.profile.analytics.ProfileTelemetry
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.GetProfileVideoViewsUseCase
import com.yral.shared.features.profile.domain.ProfileVideosPagingSource
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.VideoViews
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.LinkInput
import com.yral.shared.libs.sharing.ShareService
import com.yral.shared.reportVideo.domain.ReportRequestParams
import com.yral.shared.reportVideo.domain.ReportVideoUseCase
import com.yral.shared.reportVideo.domain.models.ReportSheetState
import com.yral.shared.reportVideo.domain.models.ReportVideoData
import com.yral.shared.rust.service.domain.models.PagedFollowerItem
import com.yral.shared.rust.service.domain.pagedDataSource.UserInfoPagingSourceFactory
import com.yral.shared.rust.service.domain.usecases.FollowUserParams
import com.yral.shared.rust.service.domain.usecases.FollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4Params
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4UseCase
import com.yral.shared.rust.service.domain.usecases.UnfollowUserParams
import com.yral.shared.rust.service.domain.usecases.UnfollowUserUseCase
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Suppress("LongParameterList", "TooManyFunctions")
class ProfileViewModel(
    private val canisterData: CanisterData,
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository,
    private val deleteVideoUseCase: DeleteVideoUseCase,
    private val reportVideoUseCase: ReportVideoUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getProfileVideoViewsUseCase: GetProfileVideoViewsUseCase,
    private val profileTelemetry: ProfileTelemetry,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val crashlyticsManager: CrashlyticsManager,
    private val flagManager: FeatureFlagManager,
    private val userInfoPagingSourceFactory: UserInfoPagingSourceFactory,
    private val getProfileDetailsV4UseCase: GetProfileDetailsV4UseCase,
) : ViewModel() {
    companion object {
        private const val POSTS_PER_PAGE = 20
        private const val POSTS_PREFETCH_DISTANCE = 5
        private val VIEWS_REFRESH_THRESHOLD = 15.seconds
    }

    private val _state =
        MutableStateFlow(
            ViewState(
                isWalletEnabled = flagManager.isEnabled(WalletFeatureFlags.Wallet.Enabled),
            ),
        )
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private val profileEventsChannel = Channel<ProfileEvents>(Channel.CONFLATED)
    val profileEvents = profileEventsChannel.receiveAsFlow()

    private val deletedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val profileVideos: Flow<PagingData<FeedDetails>> =
        if (canisterData.userPrincipalId.isNotEmpty()) {
            Pager(
                config =
                    PagingConfig(
                        pageSize = POSTS_PER_PAGE,
                        initialLoadSize = POSTS_PER_PAGE,
                        prefetchDistance = POSTS_PREFETCH_DISTANCE,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = {
                    ProfileVideosPagingSource(
                        profileRepository = profileRepository,
                        canisterId = canisterData.canisterId,
                        userPrincipal = canisterData.userPrincipalId,
                        isFromServiceCanister = canisterData.isCreatedFromServiceCanister,
                    )
                },
            ).flow
                .cachedIn(viewModelScope)
                .combine(deletedVideoIds) { pagingData, deletedIds ->
                    val videoIds = mutableSetOf<String>()
                    pagingData
                        .filter { video ->
                            video.videoID.isNotEmpty() &&
                                videoIds.add(video.videoID) &&
                                video.videoID !in deletedIds
                        }
                }.distinctUntilChanged()
        } else {
            flowOf()
        }

    val followers: Flow<PagingData<PagedFollowerItem>> =
        if (canisterData.userPrincipalId.isNotEmpty()) {
            Pager(
                config =
                    PagingConfig(
                        pageSize = POSTS_PER_PAGE,
                        initialLoadSize = POSTS_PER_PAGE,
                        prefetchDistance = POSTS_PREFETCH_DISTANCE,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = {
                    userInfoPagingSourceFactory.createFollowersPagingSource(
                        principal = canisterData.userPrincipalId,
                        targetPrincipal = canisterData.userPrincipalId,
                        withCallerFollows = true,
                    )
                },
            ).flow.cachedIn(viewModelScope)
        } else {
            emptyFlow()
        }

    val following: Flow<PagingData<PagedFollowerItem>> =
        if (canisterData.userPrincipalId.isNotEmpty()) {
            Pager(
                config =
                    PagingConfig(
                        pageSize = POSTS_PER_PAGE,
                        initialLoadSize = POSTS_PER_PAGE,
                        prefetchDistance = POSTS_PREFETCH_DISTANCE,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = {
                    userInfoPagingSourceFactory.createFollowingPagingSource(
                        principal = canisterData.userPrincipalId,
                        targetPrincipal = canisterData.userPrincipalId,
                        withCallerFollows = true,
                    )
                },
            ).flow.cachedIn(viewModelScope)
        } else {
            emptyFlow()
        }

    val followStatus = sessionManager.observeSessionProperty { it.followedPrincipals to it.unFollowedPrincipals }

    init {
        _state.update {
            it.copy(
                accountInfo =
                    AccountInfo(
                        userPrincipal = canisterData.userPrincipalId,
                        profilePic = canisterData.profilePic,
                        username = canisterData.username,
                        bio = null, // populated for own profile via session observer
                    ),
            )
        }
        val isOwnProfile = canisterData.userPrincipalId == sessionManager.userPrincipal
        if (!isOwnProfile) {
            _state.update {
                it.copy(
                    isOwnProfile = false,
                    isWalletEnabled = false,
                    isFollowing = canisterData.isFollowing,
                )
            }
        } else {
            viewModelScope.launch {
                sessionManager
                    .observeSessionState(transform = { sessionManager.getAccountInfo() })
                    .collect { info ->
                        _state.update { current ->
                            current.copy(accountInfo = info)
                        }
                    }
            }
            viewModelScope.launch {
                val principal = sessionManager.userPrincipal ?: return@launch
                getProfileDetailsV4UseCase(
                    GetProfileDetailsV4Params(
                        principal = principal,
                        targetPrincipal = principal,
                    ),
                ).onSuccess { details ->
                    val updatedPic = details.profilePictureUrl
                    if (!updatedPic.isNullOrBlank()) {
                        sessionManager.updateProfilePicture(updatedPic)
                        _state.update { current ->
                            current.accountInfo?.let { current.copy(accountInfo = it.copy(profilePic = updatedPic)) }
                                ?: current
                        }
                    }
                    details.bio.let { bio ->
                        sessionManager.updateBio(bio)
                    }
                }.onFailure { error ->
                    crashlyticsManager.recordException(Exception(error))
                }
            }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    _state.update { it.copy(isLoggedIn = isSocialSignIn) }
                }
        }
    }

    fun confirmDelete(
        feedDetails: FeedDetails,
        ctaType: VideoDeleteCTA,
    ) {
        if (_state.value.deleteConfirmation !is DeleteConfirmationState.None) return
        profileTelemetry.onVideoClicked(feedDetails)
        updateDeleteConfirmationIfDifferent(
            DeleteConfirmationState.AwaitingConfirmation(
                request =
                    DeleteVideoRequest(
                        feedDetails = feedDetails,
                        ctaType = ctaType,
                    ),
            ),
        )
    }

    fun deleteVideo() {
        val currentState = _state.value
        val deleteRequest =
            when (val deleteState = currentState.deleteConfirmation) {
                is DeleteConfirmationState.AwaitingConfirmation -> deleteState.request
                is DeleteConfirmationState.Error -> deleteState.request
                else -> return
            }
        viewModelScope.launch {
            profileTelemetry.onDeleteInitiated(
                feedDetails = deleteRequest.feedDetails,
            )
            _state.update { state ->
                state.copy(deleteConfirmation = DeleteConfirmationState.InProgress(deleteRequest))
            }
            deleteVideoUseCase
                .invoke(deleteRequest)
                .onSuccess {
                    profileTelemetry.onDeleted(
                        feedDetails = deleteRequest.feedDetails,
                        catType = deleteRequest.ctaType,
                    )
                    deletedVideoIds.update { it + deleteRequest.feedDetails.videoID }

                    // Update session manager with new video count
                    val currentCount = sessionManager.profileVideosCount
                    sessionManager.updateProfileVideosCount(
                        count = (currentCount - 1).coerceAtLeast(0),
                    )
                    _state.update { it.copy(deleteConfirmation = DeleteConfirmationState.None) }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            deleteConfirmation = DeleteConfirmationState.Error(deleteRequest, error),
                        )
                    }
                }
        }
    }

    fun clearDeleteConfirmationState() {
        updateDeleteConfirmationIfDifferent(DeleteConfirmationState.None)
    }

    fun openVideoReel(clickedIndex: Int) {
        updateVideoViewIfDifferent(VideoViewState.ViewingReels(clickedIndex))
    }

    fun closeVideoReel() {
        updateVideoViewIfDifferent(VideoViewState.None)
    }

    private fun updateDeleteConfirmationIfDifferent(newState: DeleteConfirmationState) {
        _state.update { currentState ->
            if (currentState.deleteConfirmation != newState) {
                currentState.copy(deleteConfirmation = newState)
            } else {
                currentState
            }
        }
    }

    private fun updateVideoViewIfDifferent(newState: VideoViewState) {
        _state.update { currentState ->
            if (currentState.videoView != newState) {
                currentState.copy(videoView = newState)
            } else {
                currentState
            }
        }
    }

    fun pushScreenView(totalVideos: Int) {
        profileTelemetry.onProfileScreenViewed(
            totalVideos = totalVideos,
            publisherUserId = state.value.accountInfo?.userPrincipal ?: "",
        )
    }

    fun uploadVideoClicked() {
        profileTelemetry.onUploadVideoClicked()
    }

    fun setManualRefreshTriggered(isTriggered: Boolean) {
        _state.update { it.copy(manualRefreshTriggered = isTriggered) }
        if (isTriggered && _state.value.isOwnProfile) {
            sessionManager.updateProfileVideosCount(null)
        }
    }

    fun onShareClicked(
        feedDetails: FeedDetails,
        message: String,
        description: String,
    ) {
        profileTelemetry.onShareClicked(feedDetails, sessionManager.userPrincipal)
        viewModelScope.launch {
            // Build internal deep link using UrlBuilder and PostDetailsRoute
            val route =
                PostDetailsRoute(canisterId = feedDetails.canisterID, postId = feedDetails.postID)
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
                Logger.e(ProfileViewModel::class.simpleName!!, it) { "Failed to share post" }
                crashlyticsManager.recordException(YralException(it))
            }
        }
    }

    fun setBottomSheetType(type: ProfileBottomSheet) {
        _state.update { it.copy(bottomSheet = type) }
    }

    fun getTncLink(): String = flagManager.get(AccountFeatureFlags.AccountLinks.Links).tnc

    fun toggleReportSheet(
        isOpen: Boolean,
        currentDetail: FeedDetails,
        pageNo: Int,
    ) {
        var sheetState: ReportSheetState = ReportSheetState.Closed
        if (isOpen) {
            profileTelemetry.videoClicked(
                feedDetails = currentDetail,
                ctaType = CtaType.REPORT,
            )
            sheetState = ReportSheetState.Open(pageNo)
        }
        _state.update { it.copy(reportSheetState = sheetState) }
    }

    fun reportVideo(
        pageNo: Int,
        currentFeed: FeedDetails,
        reportVideoData: ReportVideoData,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isReporting = true) }
            reportVideoUseCase
                .invoke(
                    parameter =
                        ReportRequestParams(
                            postId = currentFeed.postID,
                            videoId = currentFeed.videoID,
                            reason = reportVideoData.otherReasonText.ifEmpty { reportVideoData.reason.reason },
                            canisterID = currentFeed.canisterID,
                            principal = currentFeed.principalID,
                        ),
                ).onSuccess { _ ->
                    _state.update { it.copy(isReporting = false) }
                    with(reportVideoData) {
                        ToastManager.showToast(
                            type = ToastType.Big(successMessage.first, successMessage.second),
                            status = ToastStatus.Success,
                        )
                    }
                    profileTelemetry.videoReportedSuccessfully(currentFeed, reportVideoData.reason)
                    toggleReportSheet(false, currentFeed, pageNo)
                    // Remove video from paging source
                }.onFailure {
                    _state.update { it.copy(isReporting = false) }
                    toggleReportSheet(true, currentFeed, pageNo)
                }
        }
    }

    private fun setFollowLoading(
        principal: String,
        loading: Boolean,
    ) {
        _state.update { current ->
            val updated =
                if (loading) {
                    current.followLoading + (principal to true)
                } else {
                    current.followLoading - principal
                }
            current.copy(followLoading = updated)
        }
    }

    fun followUnfollow() {
        if (_state.value.isFollowInProgress) return
        viewModelScope.launch {
            sessionManager.userPrincipal?.let {
                val currentState = _state.value
                if (currentState.isLoggedIn) {
                    if (currentState.isFollowing) {
                        unFollow()
                    } else {
                        follow()
                    }
                }
            }
        }
    }

    fun toggleFollowForPrincipal(
        targetPrincipal: String,
        currentlyFollowing: Boolean,
    ) {
        viewModelScope.launch {
            val callerPrincipal = sessionManager.userPrincipal ?: return@launch
            setFollowLoading(targetPrincipal, true)
            try {
                if (currentlyFollowing) {
                    val result =
                        unfollowUserUseCase(
                            parameter =
                                UnfollowUserParams(
                                    principal = callerPrincipal,
                                    targetPrincipal = targetPrincipal,
                                ),
                        )
                    result.onSuccess {
                        profileEventsChannel.trySend(ProfileEvents.UnfollowedSuccessfully)
                        sessionManager.removePrincipalFromFollow(targetPrincipal)
                    }
                    result.onFailure { error ->
                        profileEventsChannel.trySend(ProfileEvents.Failed(error.message ?: "Unfollow failed"))
                    }
                } else {
                    val result =
                        followUserUseCase(
                            parameter =
                                FollowUserParams(
                                    principal = callerPrincipal,
                                    targetPrincipal = targetPrincipal,
                                ),
                        )
                    result.onSuccess {
                        profileEventsChannel.trySend(ProfileEvents.FollowedSuccessfully)
                        sessionManager.addPrincipalToFollow(targetPrincipal)
                    }
                    result.onFailure { error ->
                        profileEventsChannel.trySend(ProfileEvents.Failed(error.message ?: "Follow failed"))
                    }
                }
            } finally {
                setFollowLoading(targetPrincipal, false)
            }
        }
    }

    private suspend fun follow() {
        sessionManager.userPrincipal?.let { userPrincipal ->
            _state.update { it.copy(isFollowInProgress = true) }
            followUserUseCase(
                parameter =
                    FollowUserParams(
                        principal = userPrincipal,
                        targetPrincipal = canisterData.userPrincipalId,
                    ),
            ).onSuccess {
                _state.update { it.copy(isFollowing = true, isFollowInProgress = false) }
                profileEventsChannel.trySend(ProfileEvents.FollowedSuccessfully)
                sessionManager.addPrincipalToFollow(canisterData.userPrincipalId)
                Logger.d("Follow") { "Started following" }
            }.onFailure { e ->
                _state.update { it.copy(isFollowInProgress = false) }
                profileEventsChannel.trySend(ProfileEvents.Failed(e.message ?: "Follow failed"))
                Logger.d("Follow") { "Follow request failed $e" }
            }
        }
    }

    private suspend fun unFollow() {
        sessionManager.userPrincipal?.let { userPrincipal ->
            _state.update { it.copy(isFollowInProgress = true) }
            unfollowUserUseCase(
                parameter =
                    UnfollowUserParams(
                        principal = userPrincipal,
                        targetPrincipal = canisterData.userPrincipalId,
                    ),
            ).onSuccess {
                _state.update { it.copy(isFollowing = false, isFollowInProgress = false) }
                profileEventsChannel.trySend(ProfileEvents.UnfollowedSuccessfully)
                sessionManager.removePrincipalFromFollow(canisterData.userPrincipalId)
                Logger.d("Follow") { "Discontinued following" }
            }.onFailure { e ->
                _state.update { it.copy(isFollowInProgress = false) }
                profileEventsChannel.trySend(ProfileEvents.Failed(e.message ?: "Unfollow failed"))
                Logger.d("Follow") { "UnFollow request failed $e" }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun showVideoViews(video: FeedDetails) {
        viewModelScope.launch {
            val currentViews = _state.value.viewsData[video.videoID]
            val shouldRefresh =
                when (currentViews) {
                    is UiState.InProgress -> return@launch
                    is UiState.Success -> {
                        val now = Clock.System.now()
                        now - currentViews.data.lastFetched > VIEWS_REFRESH_THRESHOLD
                    }
                    else -> true
                }
            _state.update {
                it.copy(
                    bottomSheet = ProfileBottomSheet.VideoView(videoId = video.videoID),
                    viewsData =
                        if (shouldRefresh) {
                            it.viewsData.toMutableMap().apply { this[video.videoID] = UiState.InProgress() }
                        } else {
                            it.viewsData
                        },
                )
            }
            if (!shouldRefresh) return@launch
            getProfileVideoViewsUseCase
                .invoke(parameter = GetProfileVideoViewsUseCase.Params(videoId = listOf(video.videoID)))
                .onSuccess { views ->
                    val viewData = views.firstOrNull { view -> view.videoId == video.videoID }
                    Logger.d("VideoViews") { "Got video views $viewData" }
                    viewData?.let {
                        _state.update {
                            it.copy(
                                viewsData =
                                    it.viewsData.toMutableMap().apply {
                                        this[video.videoID] = UiState.Success(viewData)
                                    },
                            )
                        }
                    }
                }.onFailure { e ->
                    Logger.e("VideoViews") { "Failed to get video views $e" }
                    _state.update {
                        it.copy(
                            viewsData =
                                it.viewsData.toMutableMap().apply {
                                    this[video.videoID] = UiState.Failure(e)
                                },
                        )
                    }
                }
        }
    }
}

data class ViewState(
    val accountInfo: AccountInfo? = null,
    val deleteConfirmation: DeleteConfirmationState = DeleteConfirmationState.None,
    val videoView: VideoViewState = VideoViewState.None,
    val manualRefreshTriggered: Boolean = false,
    val isWalletEnabled: Boolean = false,
    val bottomSheet: ProfileBottomSheet = ProfileBottomSheet.None,
    val isReporting: Boolean = false,
    val reportSheetState: ReportSheetState = ReportSheetState.Closed,
    val isLoggedIn: Boolean = false,
    val isOwnProfile: Boolean = true,
    val isFollowing: Boolean = false,
    val isFollowInProgress: Boolean = false,
    val viewsData: Map<String, UiState<VideoViews>> = emptyMap(),
    val followLoading: Map<String, Boolean> = emptyMap(),
)

sealed interface ProfileBottomSheet {
    data object None : ProfileBottomSheet
    data object SignUp : ProfileBottomSheet
    data class VideoView(
        val videoId: String,
    ) : ProfileBottomSheet
}

sealed class DeleteConfirmationState {
    data object None : DeleteConfirmationState()
    data class AwaitingConfirmation(
        val request: DeleteVideoRequest,
    ) : DeleteConfirmationState()

    data class InProgress(
        val request: DeleteVideoRequest,
    ) : DeleteConfirmationState()

    data class Error(
        val request: DeleteVideoRequest,
        val error: Throwable,
    ) : DeleteConfirmationState()
}

sealed class VideoViewState {
    data object None : VideoViewState()
    data class ViewingReels(
        val initialPage: Int = 0,
    ) : VideoViewState()
}

sealed class ProfileEvents {
    data object FollowedSuccessfully : ProfileEvents()
    data object UnfollowedSuccessfully : ProfileEvents()
    data class Failed(
        val message: String,
    ) : ProfileEvents()
}
