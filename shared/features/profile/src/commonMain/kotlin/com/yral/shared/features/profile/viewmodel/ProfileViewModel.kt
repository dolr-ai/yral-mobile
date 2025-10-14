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
import com.yral.shared.features.profile.domain.ProfileVideosPagingSource
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.repository.ProfileRepository
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
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class ProfileViewModel(
    private val canisterData: CanisterData,
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository,
    private val deleteVideoUseCase: DeleteVideoUseCase,
    private val reportVideoUseCase: ReportVideoUseCase,
    private val profileTelemetry: ProfileTelemetry,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val crashlyticsManager: CrashlyticsManager,
    private val flagManager: FeatureFlagManager,
    private val userInfoPagingSourceFactory: UserInfoPagingSourceFactory,
) : ViewModel() {
    companion object {
        private const val POSTS_PER_PAGE = 20
        private const val POSTS_PREFETCH_DISTANCE = 5
    }

    private val _state =
        MutableStateFlow(
            ViewState(
                isWalletEnabled = flagManager.isEnabled(WalletFeatureFlags.Wallet.Enabled),
            ),
        )
    val state: StateFlow<ViewState> = _state.asStateFlow()

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

    val followers: Flow<PagingData<PagedFollowerItem>>? =
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
            flowOf()
        }

    val following: Flow<PagingData<PagedFollowerItem>>? =
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
            flowOf()
        }

    init {
        _state.update {
            it.copy(
                accountInfo =
                    AccountInfo(
                        userPrincipal = canisterData.userPrincipalId,
                        profilePic = canisterData.profilePic,
                        username = canisterData.username,
                    ),
            )
        }
        if (canisterData.userPrincipalId != sessionManager.userPrincipal) {
            _state.update {
                it.copy(
                    isOwnProfile = false,
                    isWalletEnabled = false,
                )
            }
        } else {
            viewModelScope.launch {
                sessionManager
                    .observeSessionProperties()
                    .map { it.isSocialSignIn }
                    .distinctUntilChanged()
                    .collect { isSocialSignIn ->
                        _state.update { it.copy(isLoggedIn = isSocialSignIn == true) }
                    }
            }
            viewModelScope.launch {
                sessionManager
                    .state
                    .map { sessionManager.getAccountInfo() }
                    .distinctUntilChanged()
                    .collect { info ->
                        _state.update { it.copy(accountInfo = info) }
                    }
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
                    val currentCount = sessionManager.profileVideosCount()
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
)

sealed interface ProfileBottomSheet {
    data object None : ProfileBottomSheet
    data object SignUp : ProfileBottomSheet
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
