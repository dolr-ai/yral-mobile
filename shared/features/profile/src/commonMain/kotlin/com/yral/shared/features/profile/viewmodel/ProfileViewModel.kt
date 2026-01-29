package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.shared.analytics.events.CtaType
import com.yral.shared.analytics.events.EditProfileSource
import com.yral.shared.analytics.events.FollowersListTab
import com.yral.shared.analytics.events.InfluencerClickType
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.ProDetails
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.utils.getAccountInfo
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.VideoViews
import com.yral.shared.data.domain.useCases.GetVideoViewsUseCase
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.features.profile.analytics.ProfileTelemetry
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.FollowNotificationUseCase
import com.yral.shared.features.profile.domain.ProfileVideosPagingSource
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.filedownloader.FileDownloader
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.libs.routing.routes.api.UserProfileRoute
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.LinkInput
import com.yral.shared.libs.sharing.ShareService
import com.yral.shared.reportVideo.domain.ReportRequestParams
import com.yral.shared.reportVideo.domain.ReportVideoUseCase
import com.yral.shared.reportVideo.domain.models.ReportSheetState
import com.yral.shared.reportVideo.domain.models.ReportVideoData
import com.yral.shared.rust.service.domain.models.PagedFollowerItem
import com.yral.shared.rust.service.domain.models.SubscriptionPlan
import com.yral.shared.rust.service.domain.pagedDataSource.UserInfoPagingSourceFactory
import com.yral.shared.rust.service.domain.usecases.FollowUserParams
import com.yral.shared.rust.service.domain.usecases.FollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7Params
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7UseCase
import com.yral.shared.rust.service.domain.usecases.UnfollowUserParams
import com.yral.shared.rust.service.domain.usecases.UnfollowUserUseCase
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.propicFromPrincipal
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
import org.jetbrains.compose.resources.getString
import yral_mobile.shared.features.profile.generated.resources.Res
import yral_mobile.shared.features.profile.generated.resources.download_failed
import yral_mobile.shared.features.profile.generated.resources.download_successful
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.profile_share_default_name
import yral_mobile.shared.libs.designsystem.generated.resources.something_went_wrong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class ProfileViewModel(
    private val canisterData: CanisterData,
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository,
    private val commonApis: CommonApis,
    private val deleteVideoUseCase: DeleteVideoUseCase,
    private val reportVideoUseCase: ReportVideoUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val followNotificationUseCase: FollowNotificationUseCase,
    private val getVideoViewsUseCase: GetVideoViewsUseCase,
    private val profileTelemetry: ProfileTelemetry,
    private val chatTelemetry: ChatTelemetry,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val crashlyticsManager: CrashlyticsManager,
    private val flagManager: FeatureFlagManager,
    private val userInfoPagingSourceFactory: UserInfoPagingSourceFactory,
    private val getUserProfileDetailsV7UseCase: GetUserProfileDetailsV7UseCase,
    private val getInfluencerUseCase: GetInfluencerUseCase,
    private val fileDownloader: FileDownloader,
) : ViewModel() {
    companion object {
        private const val POSTS_PER_PAGE = 20
        private const val POSTS_PREFETCH_DISTANCE = 5
        private val VIEWS_REFRESH_THRESHOLD = 15.seconds
        private val ANALYTICS_VIDEO_STARTED_RANGE = 0..1000
    }

    private val _state =
        MutableStateFlow(
            ViewState(
                isWalletEnabled = flagManager.isEnabled(WalletFeatureFlags.Wallet.Enabled),
                isSubscriptionEnabled = flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription),
            ),
        )
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private val profileEventsChannel = Channel<ProfileEvents>(Channel.CONFLATED)
    val profileEvents = profileEventsChannel.receiveAsFlow()

    // Unified state management for paging data
    private val pagingState =
        MutableStateFlow(
            PagingState(
                updatedDetails = emptyMap(),
                deletedVideoIds = emptySet(),
            ),
        )

    private val _profileVideos: Flow<PagingData<FeedDetails>> =
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
                        commonApis = commonApis,
                        canisterId = canisterData.canisterId,
                        userPrincipal = canisterData.userPrincipalId,
                        isFromServiceCanister = canisterData.isCreatedFromServiceCanister,
                    )
                },
            ).flow.cachedIn(viewModelScope)
        } else {
            flowOf()
        }

    val profileVideos: Flow<PagingData<FeedDetails>> =
        _profileVideos
            .combine(pagingState) { pagingData, state ->
                val videoIds = mutableSetOf<String>()
                pagingData
                    .filter { video ->
                        video.videoID.isNotEmpty() &&
                            videoIds.add(video.videoID) &&
                            video.videoID !in state.deletedVideoIds
                    }.map { details ->
                        state.updatedDetails[details.videoID] ?: details
                    }
            }.distinctUntilChanged()

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

    val followStatus =
        sessionManager.observeSessionProperty { it.followedPrincipals to it.unFollowedPrincipals }

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
                viewerPrincipal = sessionManager.userPrincipal,
                canShareProfile = canisterData.userPrincipalId.isNotBlank(),
            )
        }
        refreshShareCopy()
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
                    .collect { info -> setAccountInfo(info) }
            }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionState { state ->
                    when (state) {
                        is SessionState.SignedIn -> state.session.userPrincipal
                        else -> null
                    }
                }.collect { principal ->
                    _state.update { current -> current.copy(viewerPrincipal = principal) }
                }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    val wasLoggedIn = _state.value.isLoggedIn
                    _state.update { current ->
                        current.copy(
                            isLoggedIn = isSocialSignIn,
                            isOwnProfile =
                                if (isSocialSignIn) {
                                    canisterData.userPrincipalId == sessionManager.userPrincipal
                                } else {
                                    current.isOwnProfile
                                },
                        )
                    }
                    if (isSocialSignIn && !wasLoggedIn) {
                        if (canisterData.userPrincipalId == sessionManager.userPrincipal) {
                            refreshOwnProfileDetails()
                        } else {
                            refreshOtherProfileDetails()
                        }
                    }
                }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionProperty { it.proDetails }
                .collect { proDetails ->
                    proDetails?.let { details ->
                        _state.update {
                            it.copy(isProUser = details.isProPurchased)
                        }
                    }
                    Logger.d("SubscriptionX") {
                        "Prod details updated in profile $proDetails ${_state.value.isProUser}"
                    }
                }
        }
    }

    private fun setAccountInfo(info: AccountInfo?) {
        _state.update { current ->
            current.copy(
                accountInfo = info,
                canShareProfile = info?.userPrincipal?.isNotBlank() == true,
            )
        }
        refreshShareCopy()
    }

    private fun refreshShareCopy() {
        viewModelScope.launch {
            val accountInfo = _state.value.accountInfo
            val displayName =
                accountInfo?.displayName
                    ?: getString(DesignRes.string.profile_share_default_name)
            val message = getString(DesignRes.string.msg_profile_share, displayName)
            val description = getString(DesignRes.string.msg_profile_share_desc, displayName)
            _state.update { current ->
                current.copy(
                    shareDisplayName = displayName,
                    shareMessage = message,
                    shareDescription = description,
                )
            }
        }
    }

    private fun refreshOwnProfileDetails() {
        Logger.d("SubscriptionX") { "refreshOwnProfileDetails called" }
        viewModelScope.launch {
            val principal = sessionManager.userPrincipal ?: return@launch
            getUserProfileDetailsV7UseCase(
                GetUserProfileDetailsV7Params(
                    principal = principal,
                    targetPrincipal = principal,
                ),
            ).onSuccess { details ->
                val updatedPic = details.profilePictureUrl
                val bio = details.bio
                if (!updatedPic.isNullOrBlank()) {
                    sessionManager.updateProfilePicture(updatedPic)
                }
                sessionManager.updateBio(bio)
                val proPlan = details.subscriptionPlan as? SubscriptionPlan.Pro
                proPlan?.let {
                    sessionManager.updateProDetails(
                        details =
                            ProDetails(
                                isProPurchased = true,
                                availableCredits = proPlan.subscription.freeVideoCreditsLeft.toInt(),
                                totalCredits = proPlan.subscription.totalVideoCreditsAlloted.toInt(),
                            ),
                    )
                }
                _state.update { current ->
                    val currentInfo = current.accountInfo
                    val newInfo =
                        currentInfo?.copy(
                            profilePic =
                                updatedPic?.takeUnless { it.isBlank() }
                                    ?: currentInfo.profilePic,
                            bio = bio?.takeUnless { it.isBlank() } ?: currentInfo.bio,
                        )
                    current.copy(
                        accountInfo = newInfo,
                        isAiInfluencer = details.isAiInfluencer == true,
                        isProUser = proPlan != null,
                    )
                }
            }.onFailure { error ->
                Logger.e("refreshOwnProfileDetails") { "Failed to fetch profile details $error" }
            }
        }
    }

    private fun refreshOtherProfileDetails() {
        if (sessionManager.identity == null) return
        val targetPrincipal = canisterData.userPrincipalId
        if (targetPrincipal.isBlank()) return
        viewModelScope.launch {
            val callerPrincipal = sessionManager.userPrincipal ?: return@launch
            getUserProfileDetailsV7UseCase(
                GetUserProfileDetailsV7Params(
                    principal = callerPrincipal,
                    targetPrincipal = targetPrincipal,
                ),
            ).onSuccess { details ->
                _state.update { current ->
                    val existingInfo = current.accountInfo
                    val updatedInfo =
                        existingInfo?.copy(
                            bio = details.bio?.takeUnless { it.isBlank() } ?: existingInfo.bio,
                            profilePic =
                                details.profilePictureUrl?.takeUnless { it.isBlank() }
                                    ?: existingInfo.profilePic,
                        )
                    current.copy(
                        accountInfo = updatedInfo,
                        isFollowing = details.callerFollowsUser ?: current.isFollowing,
                        isAiInfluencer = details.isAiInfluencer == true,
                        isProUser = (details.subscriptionPlan as? SubscriptionPlan.Pro) != null,
                    )
                }
            }.onFailure { error ->
                Logger.e("refreshOtherProfileDetails") { "Failed to fetch profile details $error" }
            }
        }
    }

    fun fetchInfluencerDetails() {
        val influencerId = canisterData.userPrincipalId
        if (influencerId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isTalkToMeInProgress = true) }
            try {
                getInfluencerUseCase(GetInfluencerUseCase.Params(id = influencerId))
                    .onSuccess { influencer ->
                        chatTelemetry.influencerCardClicked(
                            influencerId = influencer.id,
                            influencerType = influencer.category,
                            clickType = InfluencerClickType.TALK,
                            position = 0,
                        )
                        chatTelemetry.chatInfluencerClicked(
                            influencerId = influencer.id,
                            influencerType = influencer.category,
                            source = InfluencerSource.PROFILE,
                        )
                        profileEventsChannel.trySend(ProfileEvents.InfluencerDetailsFetched(influencer))
                    }.onFailure { error ->
                        Logger.e("fetchInfluencerDetails") { "Failed to fetch influencer details $error" }
                        val message =
                            error.message
                                ?: getString(DesignRes.string.something_went_wrong)
                        profileEventsChannel.trySend(ProfileEvents.Failed(message))
                    }
            } finally {
                _state.update { it.copy(isTalkToMeInProgress = false) }
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
                    pagingState.update { current ->
                        current.copy(
                            deletedVideoIds = current.deletedVideoIds + deleteRequest.feedDetails.videoID,
                            updatedDetails = current.updatedDetails - deleteRequest.feedDetails.videoID,
                        )
                    }
                    // Update session manager with new video count
                    val currentCount = sessionManager.profileVideosCount
                    sessionManager.updateProfileVideosCount(
                        count = (currentCount - 1).coerceAtLeast(0),
                    )
                    _state.update { it.copy(deleteConfirmation = DeleteConfirmationState.None) }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            deleteConfirmation =
                                DeleteConfirmationState.Error(
                                    deleteRequest,
                                    error,
                                ),
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

    fun onEditProfileOpened(source: EditProfileSource) {
        profileTelemetry.onEditProfileStarted(source)
    }

    @Suppress("UnusedParameter")
    fun recordTime(
        currentTime: Int,
        totalTime: Int,
        feedDetails: FeedDetails,
    ) {
        if (currentTime in ANALYTICS_VIDEO_STARTED_RANGE) {
            profileTelemetry.trackVideoStarted(feedDetails)
        }
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
                Logger.e(ProfileViewModel::class.simpleName!!, it) { "Failed to share post" }
                crashlyticsManager.recordException(
                    YralException(it),
                    ExceptionType.DEEPLINK,
                )
            }
        }
    }

    fun shareProfile() {
        val principal = canisterData.userPrincipalId
        val canisterId = canisterData.canisterId
        val accountInfo = _state.value.accountInfo
        if (principal.isBlank() || canisterId.isBlank() || accountInfo == null) return
        viewModelScope.launch {
            val route =
                UserProfileRoute(
                    canisterId = canisterId,
                    userPrincipalId = principal,
                    profilePic = accountInfo.profilePic,
                    username = accountInfo.username,
                    isFromServiceCanister = canisterData.isCreatedFromServiceCanister,
                )
            val internalUrl = urlBuilder.build(route) ?: return@launch
            runSuspendCatching {
                val imageUrl =
                    accountInfo.profilePic
                        .takeIf { it.isNotBlank() }
                        ?: propicFromPrincipal(accountInfo.userPrincipal)
                val link =
                    linkGenerator.generateShareLink(
                        LinkInput(
                            internalUrl = internalUrl,
                            title = _state.value.shareMessage,
                            description = _state.value.shareDescription,
                            feature = "share_profile",
                            tags = listOf("organic", "profile_share"),
                            contentImageUrl = imageUrl,
                            metadata = mapOf("user_principal_id" to principal),
                        ),
                    )
                val text = "${_state.value.shareMessage} $link"
                shareService.shareImageWithText(
                    imageUrl = imageUrl,
                    text = text,
                )
            }.onFailure {
                Logger.e(ProfileViewModel::class.simpleName!!, it) { "Failed to share profile" }
                crashlyticsManager.recordException(
                    YralException(it),
                    ExceptionType.DEEPLINK,
                )
            }
        }
    }

    fun downloadVideo(feedDetails: FeedDetails) {
        viewModelScope.launch {
            _state.update { it.copy(bottomSheet = ProfileBottomSheet.DownloadTriggered) }
            runSuspendCatching {
                fileDownloader
                    .downloadFile(
                        url = feedDetails.url,
                        fileName = "YRAL_${feedDetails.videoID}.mp4",
                        saveToGallery = true,
                    ).onSuccess {
                        profileTelemetry.videoDownloaded(feedDetails.videoID)
                        ToastManager.showSuccess(
                            type =
                                ToastType.Small(getString(Res.string.download_successful)),
                        )
                        if (_state.value.bottomSheet == ProfileBottomSheet.DownloadTriggered) {
                            _state.update { it.copy(bottomSheet = ProfileBottomSheet.None) }
                        }
                        Logger.d("FileDownload") { "File download successful" }
                    }.onFailure { e ->
                        ToastManager.showToast(
                            type = ToastType.Small(getString(Res.string.download_failed)),
                            status = ToastStatus.Error,
                        )
                        if (_state.value.bottomSheet == ProfileBottomSheet.DownloadTriggered) {
                            _state.update { it.copy(bottomSheet = ProfileBottomSheet.None) }
                        }
                        Logger.e("FileDownload", e) { "File download failed" }
                    }
            }.onFailure { error ->
                Logger.e("FileDownload", error) { "File download failed" }
                crashlyticsManager.recordException(
                    YralException(error),
                    ExceptionType.UNKNOWN,
                )
            }
        }
    }

    fun setBottomSheetType(type: ProfileBottomSheet) {
        _state.update { it.copy(bottomSheet = type) }
    }

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
                        profileTelemetry.unFollowClicked(canisterData.userPrincipalId)
                        unFollow()
                    } else {
                        profileTelemetry.followClicked(canisterData.userPrincipalId)
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
                    profileTelemetry.unFollowClicked(targetPrincipal)
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
                        profileEventsChannel.trySend(
                            ProfileEvents.Failed(
                                error.message ?: "Unfollow failed",
                            ),
                        )
                    }
                } else {
                    profileTelemetry.followClicked(targetPrincipal)
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
                        followNotification(targetPrincipal)
                    }
                    result.onFailure { error ->
                        profileEventsChannel.trySend(
                            ProfileEvents.Failed(
                                error.message ?: "Follow failed",
                            ),
                        )
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
                followNotification(canisterData.userPrincipalId)
                Logger.d("Follow") { "Started following" }
            }.onFailure { e ->
                _state.update { it.copy(isFollowInProgress = false) }
                profileEventsChannel.trySend(ProfileEvents.Failed(e.message ?: "Follow failed"))
                Logger.d("Follow") { "Follow request failed $e" }
            }
        }
    }

    private fun followNotification(targetPrincipal: String) {
        viewModelScope.launch {
            // fire and forget
            sessionManager.username?.let { userName ->
                followNotificationUseCase(
                    parameter =
                        FollowNotificationUseCase.Params(
                            followerUsername = userName,
                            targetPrincipal = targetPrincipal,
                        ),
                )
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

    @Suppress("LongMethod")
    @OptIn(ExperimentalTime::class)
    fun showVideoViews(video: FeedDetails) {
        if (!_state.value.isOwnProfile) return
        viewModelScope.launch {
            val shouldRefresh =
                when (val currentViews = _state.value.viewsData[video.videoID]) {
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
                            it.viewsData
                                .toMutableMap()
                                .apply { this[video.videoID] = UiState.InProgress() }
                        } else {
                            it.viewsData
                        },
                )
            }
            if (!shouldRefresh) return@launch
            getVideoViewsUseCase
                .invoke(parameter = GetVideoViewsUseCase.Params(videoId = listOf(video.videoID)))
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
                        pagingState.update { current ->
                            current.copy(
                                updatedDetails =
                                    current.updatedDetails +
                                        (
                                            video.videoID to
                                                video.copy(
                                                    viewCount = viewData.allViews,
                                                    bulkViewCount = viewData.allViews,
                                                )
                                        ),
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

    fun updateFollowSheetTab(tab: FollowersSheetTab) {
        _state.update {
            it.copy(
                bottomSheet = ProfileBottomSheet.FollowDetails(tab),
            )
        }
    }

    fun followListViewed(
        tab: FollowersSheetTab,
        totalCount: Long,
    ) {
        profileTelemetry.followerListViewed(
            publisherUserId = canisterData.userPrincipalId,
            totalCount = totalCount,
            tab =
                when (tab) {
                    FollowersSheetTab.Followers -> FollowersListTab.FOLLOWERS
                    FollowersSheetTab.Following -> FollowersListTab.FOLLOWING
                },
        )
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
    val viewerPrincipal: String? = null,
    val shareDisplayName: String = "",
    val shareMessage: String = "",
    val shareDescription: String = "",
    val canShareProfile: Boolean = false,
    val isAiInfluencer: Boolean = false,
    val isTalkToMeInProgress: Boolean = false,
    val isProUser: Boolean = false,
    val isSubscriptionEnabled: Boolean = false,
)

sealed interface ProfileBottomSheet {
    data object None : ProfileBottomSheet
    data class VideoView(
        val videoId: String,
    ) : ProfileBottomSheet
    data class FollowDetails(
        val tab: FollowersSheetTab,
    ) : ProfileBottomSheet
    data object DownloadTriggered : ProfileBottomSheet
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
    data class InfluencerDetailsFetched(
        val influencer: Influencer,
    ) : ProfileEvents()
    data class Failed(
        val message: String,
    ) : ProfileEvents()
}

data class PagingState(
    val updatedDetails: Map<String, FeedDetails>,
    val deletedVideoIds: Set<String>,
)

enum class FollowersSheetTab {
    Followers,
    Following,
}
