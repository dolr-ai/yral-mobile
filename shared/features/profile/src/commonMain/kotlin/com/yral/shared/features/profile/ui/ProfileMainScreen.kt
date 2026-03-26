@file:Suppress("TooManyFunctions")

package com.yral.shared.features.profile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.yral.shared.analytics.events.EditProfileSource
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.data.domain.models.ConversationInfluencerSource
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.auth.ui.LoginMode
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.features.auth.ui.rememberLoginInfo
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.profile.ui.followers.FollowersBottomSheet
import com.yral.shared.features.profile.viewmodel.DeleteConfirmationState
import com.yral.shared.features.profile.viewmodel.FollowersSheetTab
import com.yral.shared.features.profile.viewmodel.ProfileBottomSheet
import com.yral.shared.features.profile.viewmodel.ProfileEvents
import com.yral.shared.features.profile.viewmodel.ProfileTab
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.features.profile.viewmodel.VideoViewState
import com.yral.shared.features.profile.viewmodel.ViewState
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.LoaderSize
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralContextMenu
import com.yral.shared.libs.designsystem.component.YralContextMenuItem
import com.yral.shared.libs.designsystem.component.YralDragHandle
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.YralLoadingDots
import com.yral.shared.libs.designsystem.component.features.AccountInfoView
import com.yral.shared.libs.designsystem.component.features.DeleteConfirmationSheet
import com.yral.shared.libs.designsystem.component.features.SubscribeButton
import com.yral.shared.libs.designsystem.component.features.VideoViewsSheet
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showError
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import com.yral.shared.libs.videoPlayer.YralVideoPlayer
import com.yral.shared.rust.service.domain.models.FollowerItem
import com.yral.shared.rust.service.domain.models.PagedFollowerItem
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import com.yral.shared.rust.service.utils.propicFromPrincipal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import yral_mobile.shared.features.profile.generated.resources.Res
import yral_mobile.shared.features.profile.generated.resources.become_pro
import yral_mobile.shared.features.profile.generated.resources.create_ai_video
import yral_mobile.shared.features.profile.generated.resources.delete
import yral_mobile.shared.features.profile.generated.resources.delete_video
import yral_mobile.shared.features.profile.generated.resources.deleting
import yral_mobile.shared.features.profile.generated.resources.download
import yral_mobile.shared.features.profile.generated.resources.downloading_video
import yral_mobile.shared.features.profile.generated.resources.downloading_video_desc
import yral_mobile.shared.features.profile.generated.resources.draft_label
import yral_mobile.shared.features.profile.generated.resources.drafts_empty_subtitle
import yral_mobile.shared.features.profile.generated.resources.drafts_empty_title
import yral_mobile.shared.features.profile.generated.resources.error_loading_more_videos
import yral_mobile.shared.features.profile.generated.resources.error_loading_videos
import yral_mobile.shared.features.profile.generated.resources.failed_to_delete_video
import yral_mobile.shared.features.profile.generated.resources.ic_drafts_selected
import yral_mobile.shared.features.profile.generated.resources.ic_drafts_unselected
import yral_mobile.shared.features.profile.generated.resources.ic_published_selected
import yral_mobile.shared.features.profile.generated.resources.ic_published_unselected
import yral_mobile.shared.features.profile.generated.resources.pink_heart
import yral_mobile.shared.features.profile.generated.resources.pro_profile_background
import yral_mobile.shared.features.profile.generated.resources.profile_empty_other_subtitle
import yral_mobile.shared.features.profile.generated.resources.profile_empty_other_title
import yral_mobile.shared.features.profile.generated.resources.profile_empty_subtitle
import yral_mobile.shared.features.profile.generated.resources.profile_empty_title
import yral_mobile.shared.features.profile.generated.resources.profile_locked_subtitle
import yral_mobile.shared.features.profile.generated.resources.profile_locked_title
import yral_mobile.shared.features.profile.generated.resources.profile_view_locked_subtitle
import yral_mobile.shared.features.profile.generated.resources.profile_view_locked_title
import yral_mobile.shared.features.profile.generated.resources.publish_button
import yral_mobile.shared.features.profile.generated.resources.publish_success
import yral_mobile.shared.features.profile.generated.resources.storage_permission_required
import yral_mobile.shared.features.profile.generated.resources.tab_new
import yral_mobile.shared.features.profile.generated.resources.video_generating
import yral_mobile.shared.features.profile.generated.resources.video_will_be_deleted_permanently
import yral_mobile.shared.features.profile.generated.resources.white_heart
import yral_mobile.shared.libs.designsystem.generated.resources.account_nav
import yral_mobile.shared.libs.designsystem.generated.resources.arrow
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.cancel
import yral_mobile.shared.libs.designsystem.generated.resources.delete
import yral_mobile.shared.libs.designsystem.generated.resources.error_data_not_loaded
import yral_mobile.shared.libs.designsystem.generated.resources.ic_dots_vertical
import yral_mobile.shared.libs.designsystem.generated.resources.ic_download
import yral_mobile.shared.libs.designsystem.generated.resources.ic_share
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import yral_mobile.shared.libs.designsystem.generated.resources.ic_views
import yral_mobile.shared.libs.designsystem.generated.resources.login
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.my_profile
import yral_mobile.shared.libs.designsystem.generated.resources.my_profiles
import yral_mobile.shared.libs.designsystem.generated.resources.oops
import yral_mobile.shared.libs.designsystem.generated.resources.refresh
import yral_mobile.shared.libs.designsystem.generated.resources.share_profile
import yral_mobile.shared.libs.designsystem.generated.resources.something_went_wrong
import yral_mobile.shared.libs.designsystem.generated.resources.started_following
import yral_mobile.shared.libs.designsystem.generated.resources.try_again
import yral_mobile.shared.libs.designsystem.generated.resources.video_insights
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

internal const val GRID_ITEM_ASPECT_RATIO = 0.75f
internal const val PULL_TO_REFRESH_INDICATOR_SIZE = 34f
internal const val PULL_TO_REFRESH_INDICATOR_THRESHOLD = 36f
internal const val PULL_TO_REFRESH_OFFSET_MULTIPLIER = 1.5f
internal const val MAX_LINES_FOR_POST_DESCRIPTION = 5
internal const val PADDING_BOTTOM_ACCOUNT_INFO = 20
internal const val SHEET_GROWTH_PER_ITEM = 0.05f
internal const val PROFILE_SWITCHER_ROTATION_DEGREES = 90f

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ProfileMainScreen(
    component: ProfileMainComponent,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    profileVideos: LazyPagingItems<FeedDetails>,
) {
    val sessionManager: SessionManager = koinInject()
    // Use nullable to avoid showing CTA flash while loading
    val botCount by
        sessionManager
            .observeSessionProperty { it.botCount }
            .collectAsStateWithLifecycle(initialValue = null)
    val accountDirectory by
        sessionManager
            .observeSessionProperty { it.accountDirectory }
            .collectAsStateWithLifecycle(initialValue = null)
    val hasBotAccounts = (botCount ?: 0) > 0
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showCreateBotCta by
        sessionManager
            .shouldShowCreateBotCtaFlow(state.maxBotCountForCta)
            .collectAsStateWithLifecycle(initialValue = false)
    val storagePermissionController = rememberStoragePermissionController()
    val isBotAccount = sessionManager.isBotAccount == true

    val followers = viewModel.followers.collectAsLazyPagingItems()
    val following = viewModel.following.collectAsLazyPagingItems()
    val draftVideos = viewModel.draftVideos.collectAsLazyPagingItems()

    val loginState = rememberLoginInfo(requestLoginFactory = component.requestLoginFactory)

    // Track pending downloads for permission handling
    var pendingDownload by remember { mutableStateOf<FeedDetails?>(null) }

    // Handle permission check before download
    LaunchedEffect(pendingDownload) {
        val feedDetails = pendingDownload ?: return@LaunchedEffect
        pendingDownload = null // Clear immediately to prevent re-trigger

        val hasPermission = storagePermissionController.isPermissionGranted()
        if (!hasPermission) {
            val granted = storagePermissionController.requestPermission()
            if (!granted) {
                ToastManager.showError(
                    type = ToastType.Small(getString(Res.string.storage_permission_required)),
                )
                return@LaunchedEffect
            }
        }
        // Permission granted, proceed with download
        viewModel.downloadVideo(feedDetails)
    }

    // Wrapper function for download that triggers permission check
    val onDownloadVideo: (FeedDetails) -> Unit = { feedDetails ->
        pendingDownload = feedDetails
    }

    val followedSuccessfully =
        stringResource(DesignRes.string.started_following, state.accountInfo?.displayName ?: "")
    LaunchedEffect(Unit) {
        viewModel.profileEvents.collect { event ->
            when (event) {
                is ProfileEvents.FollowedSuccessfully -> {
                    ToastManager.showSuccess(type = ToastType.Small(message = followedSuccessfully))
                    component.showAlertsOnDialog(AlertsRequestType.FOLLOW_BACK)
                    if (state.isOwnProfile) {
                        following.refresh()
                    } else {
                        followers.refresh()
                    }
                }

                is ProfileEvents.UnfollowedSuccessfully -> {
                    if (state.isOwnProfile) {
                        following.refresh()
                    } else {
                        followers.refresh()
                    }
                }

                is ProfileEvents.InfluencerDetailsFetched -> {
                    component.openConversation(
                        OpenConversationParams(
                            influencerId = event.influencer.id,
                            influencerCategory = event.influencer.category,
                            influencerSource = ConversationInfluencerSource.PROFILE,
                        ),
                    )
                }

                is ProfileEvents.SubscribeInfluencerDetailsFetched -> {
                    component.openConversation(
                        OpenConversationParams(
                            influencerId = event.influencer.id,
                            influencerCategory = event.influencer.category,
                            influencerSource = ConversationInfluencerSource.PROFILE,
                            autoTriggerPurchase = true,
                        ),
                    )
                }

                is ProfileEvents.Failed -> {
                    ToastManager.showError(type = ToastType.Small(message = event.message))
                }

                is ProfileEvents.RefreshDrafts -> {
                    ToastManager.showSuccess(
                        type = ToastType.Small(getString(Res.string.publish_success)),
                    )
                    profileVideos.refresh()
                    draftVideos.refresh()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.followStatus.collect {
            if (state.isOwnProfile) {
                followers.refresh()
                following.refresh()
            }
        }
    }

    val backHandlerEnabled by remember(state.videoView) {
        mutableStateOf(state.videoView is VideoViewState.ViewingReels || state.videoView is VideoViewState.ViewingDraft)
    }

    LaunchedEffect(Unit) { viewModel.pushScreenView(profileVideos.itemCount) }

    LifecycleResumeEffect(Unit) {
        viewModel.recheckSubscriptionIfNeeded()
        onPauseOrDispose {}
    }

    var pendingVideoIdState by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        component.pendingVideoNavigation.collectLatest {
            if (it != null) {
                pendingVideoIdState = it
                profileVideos.refresh()
            }
        }
    }

    LaunchedEffect(profileVideos.loadState.refresh, pendingVideoIdState) {
        if (profileVideos.loadState.refresh is LoadState.NotLoading) {
            val pendingVideoId = pendingVideoIdState
            if (pendingVideoId != null) {
                pendingVideoIdState = null
                val itemCount = profileVideos.itemCount
                if (itemCount > 0) {
                    val videoIndex =
                        (0 until itemCount).indexOfFirst { index ->
                            profileVideos[index]?.videoID == pendingVideoId
                        }
                    viewModel.openVideoReel(videoIndex.coerceAtLeast(0))
                }
            }
        }
    }

    BackHandler(
        enabled = backHandlerEnabled,
        onBack = {
            when (state.videoView) {
                is VideoViewState.ViewingDraft -> viewModel.closeDraftVideo()
                is VideoViewState.ViewingReels -> viewModel.closeVideoReel()
                else -> {}
            }
        },
    )

    val deletingVideoId =
        remember(state.deleteConfirmation) {
            when (val deleteConfirmation = state.deleteConfirmation) {
                is DeleteConfirmationState.InProgress -> deleteConfirmation.request.feedDetails.videoID
                else -> ""
            }
        }

    val gridState = rememberLazyGridState()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .then(
                    if (state.isProUser) {
                        Modifier.paint(
                            painter = painterResource(Res.drawable.pro_profile_background),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        when (val videoViewState = state.videoView) {
            is VideoViewState.ViewingReels -> {
                if (profileVideos.itemCount > 0) {
                    val msgFeedVideoShare = stringResource(DesignRes.string.msg_feed_video_share)
                    val msgFeedVideoShareDesc =
                        stringResource(DesignRes.string.msg_feed_video_share_desc)
                    ProfileReelPlayer(
                        reelVideos = profileVideos,
                        initialPage =
                            videoViewState.initialPage
                                .coerceAtMost(profileVideos.itemCount - 1),
                        isOwnProfile = state.isOwnProfile,
                        userName = state.accountInfo?.displayName,
                        deletingVideoId = deletingVideoId,
                        onBack = { viewModel.closeVideoReel() },
                        isReporting = state.isReporting,
                        reportSheetState = state.reportSheetState,
                        onReportClick = { pageNo, video ->
                            viewModel.toggleReportSheet(
                                isOpen = true,
                                currentDetail = video,
                                pageNo = pageNo,
                            )
                        },
                        dismissReportSheet = { video ->
                            viewModel.toggleReportSheet(
                                isOpen = false,
                                currentDetail = video,
                                pageNo = 0,
                            )
                        },
                        reportVideo = { pageNo, video, reportVideoData ->
                            viewModel.reportVideo(
                                pageNo = pageNo,
                                currentFeed = video,
                                reportVideoData = reportVideoData,
                            )
                        },
                        onDeleteVideo = { video ->
                            viewModel.confirmDelete(
                                feedDetails = video,
                                ctaType = VideoDeleteCTA.VIDEO_FULLSCREEN,
                            )
                        },
                        onDownloadVideo = onDownloadVideo,
                        onShareClick = { feedDetails ->
                            viewModel.onShareClicked(
                                feedDetails,
                                msgFeedVideoShare,
                                msgFeedVideoShareDesc,
                            )
                        },
                        onViewsClick = { video -> viewModel.showVideoViews(video) },
                        onRecordTime = { currentTime, totalTime, video ->
                            viewModel.recordTime(currentTime, totalTime, video)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    viewModel.closeVideoReel()
                }
            }

            is VideoViewState.ViewingDraft -> {
                DraftVideoDetailScreen(
                    feedDetails = videoViewState.feedDetails,
                    isPublishing = state.publishDraftUiState is UiState.InProgress,
                    onBack = { viewModel.closeDraftVideo() },
                    onPublish = { viewModel.publishDraft(videoViewState.feedDetails) },
                )
            }

            VideoViewState.None -> {
                MainContent(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    viewModel = viewModel,
                    gridState = gridState,
                    profileVideos = profileVideos,
                    draftVideos = draftVideos,
                    followers = followers,
                    following = following,
                    deletingVideoId = deletingVideoId,
                    uploadVideo = {
                        viewModel.uploadVideoClicked()
                        component.onUploadVideoClick()
                    },
                    openAccountSheet = { component.openAccountSheet() },
                    openAccount = { component.openAccount() },
                    openEditProfile = {
                        viewModel.onEditProfileOpened(EditProfileSource.PROFILE)
                        component.openEditProfile()
                    },
                    onBackClicked = { component.onBackClicked() },
                    onFollowersSectionClick = { viewModel.updateFollowSheetTab(tab = it) },
                    promptLogin = {
                        loginState.requestLogin(
                            SignupPageName.PROFILE,
                            LoginScreenType.BottomSheet(LoginBottomSheetType.DEFAULT),
                            LoginMode.BOTH,
                            null,
                            null,
                        ) {}
                    },
                    canShareProfile = state.canShareProfile,
                    onShareProfileClicked = { viewModel.shareProfile() },
                    showHeaderShareButton = !state.isOwnProfile,
                    showBackButton = component.showBackButton,
                    onDownloadVideo = onDownloadVideo,
                    onSubscribe = {
                        if (state.isLoggedIn) {
                            component.subscriptionCoordinator.buySubscription()
                        } else {
                            loginState.requestLogin(
                                SignupPageName.PROFILE,
                                LoginScreenType.BottomSheet(LoginBottomSheetType.DEFAULT),
                                LoginMode.BOTH,
                                null,
                                null,
                            ) {}
                        }
                    },
                    isBotAccount = isBotAccount,
                    hasBotAccounts = hasBotAccounts,
                    showCreateBotCta = showCreateBotCta,
                    onCreateInfluencerClick = {
                        viewModel.trackCreateInfluencerClicked()
                        component.openCreateInfluencer()
                    },
                    onUsernameClick = { username ->
                        val principal =
                            state.botUsernameToCanisterData[username]
                                ?: state.createdByPrincipal.takeIf { username == state.createdByUsername }
                                ?: return@MainContent
                        val canisterData =
                            CanisterData(
                                canisterId = getUserInfoServiceCanister(),
                                userPrincipalId = principal,
                                profilePic = propicFromPrincipal(principal),
                                username = username,
                                isCreatedFromServiceCanister = true,
                            )
                        component.openProfile(canisterData)
                    },
                )
            }
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    when (state.deleteConfirmation) {
        is DeleteConfirmationState.AwaitingConfirmation -> {
            DeleteConfirmationSheet(
                bottomSheetState = bottomSheetState,
                title = stringResource(Res.string.delete_video),
                subTitle = "",
                confirmationMessage = stringResource(Res.string.video_will_be_deleted_permanently),
                cancelButton = stringResource(DesignRes.string.cancel),
                deleteButton = stringResource(Res.string.delete),
                onDismissRequest = { viewModel.clearDeleteConfirmationState() },
                onDelete = { viewModel.deleteVideo() },
            )
        }

        is DeleteConfirmationState.Error -> {
            YralErrorMessage(
                title = stringResource(DesignRes.string.oops),
                error = stringResource(Res.string.failed_to_delete_video),
                cta = stringResource(DesignRes.string.try_again),
                onDismiss = { viewModel.clearDeleteConfirmationState() },
                onClick = { viewModel.deleteVideo() },
                sheetState = bottomSheetState,
            )
        }

        DeleteConfirmationState.None, is DeleteConfirmationState.InProgress -> Unit
    }

    val followersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    when (val bottomSheet = state.bottomSheet) {
        ProfileBottomSheet.None -> Unit

        is ProfileBottomSheet.VideoView -> {
            val videoId = bottomSheet.videoId
            val video = profileVideos.itemSnapshotList.firstOrNull { it?.videoID == videoId }
            when (val views = state.viewsData[videoId]) {
                is UiState.Failure -> {
                    YralErrorMessage(
                        title = stringResource(DesignRes.string.video_insights),
                        error = stringResource(DesignRes.string.error_data_not_loaded),
                        cta = stringResource(DesignRes.string.refresh),
                        onDismiss = { viewModel.setBottomSheetType(ProfileBottomSheet.None) },
                        onClick = { video?.let { viewModel.showVideoViews(video) } },
                        sheetState = bottomSheetState,
                        showErrorIcon = true,
                        showDragHandle = true,
                    )
                }

                UiState.Initial,
                is UiState.InProgress,
                is UiState.Success,
                -> {
                    val totalViews = (views as? UiState.Success)?.data?.allViews
                    val totalEngagedViews = (views as? UiState.Success)?.data?.loggedInViews
                    VideoViewsSheet(
                        sheetState = bottomSheetState,
                        onDismissRequest = { viewModel.setBottomSheetType(ProfileBottomSheet.None) },
                        thumbnailUrl = video?.thumbnail ?: "",
                        totalViews = totalViews,
                        totalEngagedViews = totalEngagedViews,
                    )
                }

                else -> Unit
            }
        }

        is ProfileBottomSheet.FollowDetails -> {
            state.accountInfo?.let { accountInfo ->
                val isFetched = remember(bottomSheet.tab) { mutableStateOf(false) }
                LaunchedEffect(isFetched) {
                    snapshotFlow {
                        when (bottomSheet.tab) {
                            FollowersSheetTab.Followers -> {
                                followers.loadState.refresh is LoadState.NotLoading && followers.itemCount >= 0
                            }
                            FollowersSheetTab.Following -> {
                                following.loadState.refresh is LoadState.NotLoading && following.itemCount >= 0
                            }
                        }
                    }.distinctUntilChanged()
                        .collect { loaded ->
                            if (loaded) {
                                // Call only if not already fetched
                                if (!isFetched.value) {
                                    isFetched.value = true
                                    val count = followersCount(bottomSheet.tab, followers, following)
                                    viewModel.followListViewed(
                                        tab = bottomSheet.tab,
                                        totalCount = count.second,
                                    )
                                }
                            }
                        }
                }
                val serviceCanisterId = remember { getUserInfoServiceCanister() }
                val onFollowerSelected: (FollowerItem) -> Unit = followerSelected@{ follower ->
                    val principal = follower.principalId.toString()
                    if (principal.isBlank()) return@followerSelected
                    val profilePic =
                        follower.profilePictureUrl?.takeIf { it.isNotBlank() } ?: propicFromPrincipal(principal)
                    val canisterData =
                        CanisterData(
                            canisterId = serviceCanisterId,
                            userPrincipalId = principal,
                            profilePic = profilePic,
                            username = follower.username,
                            isCreatedFromServiceCanister = true,
                            isFollowing = follower.callerFollows,
                        )
                    viewModel.setBottomSheetType(ProfileBottomSheet.None)
                    component.openProfile(canisterData)
                }
                FollowersBottomSheet(
                    sheetState = followersSheetState,
                    onDismissRequest = { viewModel.setBottomSheetType(ProfileBottomSheet.None) },
                    username = accountInfo.displayName,
                    initialTab = bottomSheet.tab,
                    followers = followers,
                    following = following,
                    followLoading = state.followLoading,
                    viewerPrincipal = state.viewerPrincipal,
                    onTabSelected = { viewModel.updateFollowSheetTab(tab = it) },
                    onFollowToggle = viewModel::toggleFollowForPrincipal,
                    onUserSelected = onFollowerSelected,
                )
            }
        }

        is ProfileBottomSheet.DownloadTriggered -> {
            DownloadTriggeredSheet(
                bottomSheetState = bottomSheetState,
                onDismissRequest = { viewModel.setBottomSheetType(ProfileBottomSheet.None) },
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    modifier: Modifier,
    state: ViewState,
    viewModel: ProfileViewModel,
    gridState: LazyGridState,
    profileVideos: LazyPagingItems<FeedDetails>,
    draftVideos: LazyPagingItems<FeedDetails>,
    followers: LazyPagingItems<PagedFollowerItem>?,
    following: LazyPagingItems<PagedFollowerItem>?,
    deletingVideoId: String,
    uploadVideo: () -> Unit,
    openAccountSheet: () -> Unit,
    openAccount: () -> Unit,
    openEditProfile: () -> Unit,
    onBackClicked: () -> Unit,
    onFollowersSectionClick: (FollowersSheetTab) -> Unit,
    promptLogin: () -> Unit,
    canShareProfile: Boolean,
    onShareProfileClicked: () -> Unit,
    showHeaderShareButton: Boolean,
    showBackButton: Boolean,
    onDownloadVideo: (FeedDetails) -> Unit,
    onSubscribe: () -> Unit,
    isBotAccount: Boolean,
    hasBotAccounts: Boolean,
    showCreateBotCta: Boolean,
    onCreateInfluencerClick: () -> Unit,
    onUsernameClick: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ProfileHeader(
            isOwnProfile = state.isOwnProfile,
            isSubscriptionEnabled = state.isSubscriptionEnabled,
            isYralProAvailable = state.isYralProAvailable,
            isProUser = state.isProUser,
            userName = state.accountInfo?.displayName,
            showAccountChevron = state.isOwnProfile && (isBotAccount || hasBotAccounts),
            showShareProfile = showHeaderShareButton && canShareProfile,
            isWalletEnabled = state.isWalletEnabled,
            onShareProfileClicked = onShareProfileClicked,
            openAccountSheet = openAccountSheet,
            openAccount = openAccount,
            onBack = onBackClicked,
            showBackButton = showBackButton,
            onSubscribe = onSubscribe,
            showSubscribeButton =
                !state.isOwnProfile &&
                    state.isAiInfluencer &&
                    state.isLoggedIn &&
                    !state.isSubscribedToInfluencer,
            isSubscribeLoading = state.isTalkToMeInProgress,
            onSubscribeClicked = { viewModel.onSubscribeClicked() },
        )
        state.accountInfo?.let { info ->
            val followersCount = totalCount(followers)
            val followingCount = totalCount(following)
            val createdByUsername = state.createdByUsername
            AccountInfoView(
                accountInfo = info,
                totalFollowers = followersCount,
                totalFollowing = followingCount,
                isSocialSignIn = state.isLoggedIn,
                showLogin = false,
                bio = info.bio,
                showEditProfile = state.isOwnProfile && state.isLoggedIn,
                onEditProfileClicked = openEditProfile,
                showShareProfile = state.isOwnProfile && state.isLoggedIn && state.canShareProfile,
                onShareProfileClicked = onShareProfileClicked,
                showFollow = !state.isOwnProfile && state.isLoggedIn,
                isFollowing = state.isFollowing,
                isFollowInProgress = state.isFollowInProgress,
                isAiInfluencer = state.isAiInfluencer,
                isTalkToMeInProgress = state.isTalkToMeInProgress,
                onFollowClicked = { viewModel.followUnfollow() },
                onFollowersClick = { onFollowersSectionClick(FollowersSheetTab.Followers) },
                onFollowingClick = { onFollowersSectionClick(FollowersSheetTab.Following) },
                onTalkToMeClicked = viewModel::fetchInfluencerDetails,
                showSubscribe = false,
                onSubscribeClicked = {},
                isProUser = state.isProUser,
                showCreateInfluencerCta = state.isOwnProfile && state.isLoggedIn && showCreateBotCta,
                onCreateInfluencerClick = onCreateInfluencerClick,
                botUsernames = state.botUsernames,
                createdByUsername = createdByUsername,
                maxVisibleBotUsernames = state.maxVisibleBotUsernames,
                onUsernameClick = onUsernameClick,
            )
        }
        when (profileVideos.loadState.refresh) {
            is LoadState.Loading -> {
                LoadingContent()
            }

            is LoadState.Error -> {
                if (state.manualRefreshTriggered) viewModel.setManualRefreshTriggered(false)
                ErrorContent(message = stringResource(Res.string.error_loading_videos))
            }

            is LoadState.NotLoading -> {
                if (!state.isLoggedIn) {
                    LockedProfileContent(
                        onLoginClick = promptLogin,
                        isOwnProfile = state.isOwnProfile,
                    )
                } else {
                    if (state.manualRefreshTriggered) {
                        viewModel.pushScreenView(profileVideos.itemCount)
                        viewModel.setManualRefreshTriggered(false)
                    }
                    SuccessContent(
                        gridState = gridState,
                        profileVideos = profileVideos,
                        draftVideos = draftVideos,
                        selectedTab = state.selectedTab,
                        isOwnProfile = state.isOwnProfile,
                        deletingVideoId = deletingVideoId,
                        uploadVideo = uploadVideo,
                        openVideoReel = { viewModel.openVideoReel(it) },
                        openDraftVideo = { viewModel.openDraftVideo(it) },
                        onDeleteVideo = {
                            viewModel.confirmDelete(
                                feedDetails = it,
                                ctaType = VideoDeleteCTA.PROFILE_THUMBNAIL,
                            )
                        },
                        onDownloadVideo = onDownloadVideo,
                        onViewsClick = { viewModel.showVideoViews(it) },
                        onManualRefreshTriggered = { viewModel.setManualRefreshTriggered(it) },
                        onTabSelected = { viewModel.selectTab(it) },
                    )
                }
            }
        }
    }
}

private fun followersCount(
    tab: FollowersSheetTab,
    followers: LazyPagingItems<PagedFollowerItem>?,
    following: LazyPagingItems<PagedFollowerItem>?,
) = when (tab) {
    FollowersSheetTab.Followers -> (followers?.itemCount ?: 0) to totalCount(followers)
    FollowersSheetTab.Following -> (following?.itemCount ?: 0) to totalCount(following)
}

private fun totalCount(data: LazyPagingItems<PagedFollowerItem>?) =
    data?.let { pagingItems ->
        if (pagingItems.itemCount > 0) {
            pagingItems[0]?.totalCount?.toLong() ?: 0
        } else {
            0
        }
    } ?: 0

@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
@Composable
private fun ProfileHeader(
    isOwnProfile: Boolean,
    isProUser: Boolean,
    isSubscriptionEnabled: Boolean,
    isYralProAvailable: Boolean,
    userName: String?,
    showAccountChevron: Boolean,
    showShareProfile: Boolean,
    isWalletEnabled: Boolean,
    onShareProfileClicked: () -> Unit,
    openAccountSheet: () -> Unit,
    openAccount: () -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean = false,
    onSubscribe: () -> Unit,
    showSubscribeButton: Boolean = false,
    isSubscribeLoading: Boolean = false,
    onSubscribeClicked: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) {
            if (showBackButton || !isOwnProfile) {
                Icon(
                    painter = painterResource(DesignRes.drawable.arrow_left),
                    contentDescription = "back",
                    tint = Color.White,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable { onBack() },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.clickable(enabled = isOwnProfile && showAccountChevron) {
                        openAccountSheet()
                    },
            ) {
                Text(
                    text =
                        if (isOwnProfile) {
                            if (showAccountChevron) {
                                stringResource(DesignRes.string.my_profiles)
                            } else {
                                stringResource(DesignRes.string.my_profile)
                            }
                        } else {
                            userName ?: ""
                        },
                    style = LocalAppTopography.current.xlBold,
                    color = YralColors.NeutralTextPrimary,
                )
                if (isOwnProfile && showAccountChevron) {
                    Icon(
                        painter = painterResource(DesignRes.drawable.arrow),
                        contentDescription = "Profile switcher",
                        tint = Color.White,
                        modifier =
                            Modifier
                                .padding(start = 6.dp)
                                .size(20.dp)
                                .rotate(PROFILE_SWITCHER_ROTATION_DEGREES),
                    )
                }
            }
        }
        val shouldShowBecomeProButton =
            isOwnProfile && isSubscriptionEnabled && isYralProAvailable && !isProUser
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (shouldShowBecomeProButton) {
                BecomeProButton { onSubscribe() }
            }
            if (showSubscribeButton) {
                SubscribeButton(
                    modifier = Modifier.width(88.dp).height(29.dp),
                    isLoading = isSubscribeLoading,
                    onClick = onSubscribeClicked,
                )
            }
            if (showShareProfile) {
                Icon(
                    painter = painterResource(DesignRes.drawable.ic_share),
                    contentDescription = stringResource(DesignRes.string.share_profile),
                    tint = Color.White,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable { onShareProfileClicked() },
                )
            }
            if (isWalletEnabled) {
                Icon(
                    painter = painterResource(DesignRes.drawable.account_nav),
                    contentDescription = "Account",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).clickable { openAccount() },
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        YralLoader(size = PULL_TO_REFRESH_INDICATOR_SIZE.dp)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(DesignRes.string.something_went_wrong),
            style = LocalAppTopography.current.lgBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = LocalAppTopography.current.lgMedium,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    gridState: LazyGridState,
    profileVideos: LazyPagingItems<FeedDetails>,
    draftVideos: LazyPagingItems<FeedDetails>,
    selectedTab: ProfileTab,
    isOwnProfile: Boolean,
    deletingVideoId: String,
    uploadVideo: () -> Unit,
    openVideoReel: (Int) -> Unit,
    openDraftVideo: (FeedDetails) -> Unit,
    onDeleteVideo: (FeedDetails) -> Unit,
    onDownloadVideo: (FeedDetails) -> Unit,
    onViewsClick: (FeedDetails) -> Unit,
    onManualRefreshTriggered: (Boolean) -> Unit,
    onTabSelected: (ProfileTab) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = PADDING_BOTTOM_ACCOUNT_INFO.dp),
    ) {
        if (isOwnProfile) {
            ProfileTabBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                hasDrafts = draftVideos.itemCount > 0,
            )
        }

        val pullRefreshState = rememberPullToRefreshState()
        val offset =
            pullRefreshState.distanceFraction *
                PULL_TO_REFRESH_INDICATOR_SIZE *
                PULL_TO_REFRESH_OFFSET_MULTIPLIER

        val activeVideos = if (selectedTab == ProfileTab.Drafts) draftVideos else profileVideos
        val isRefreshing = activeVideos.loadState.refresh is LoadState.Loading

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                onManualRefreshTriggered(true)
                activeVideos.refresh()
            },
            state = pullRefreshState,
            indicator = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .pullToRefreshIndicator(
                                state = pullRefreshState,
                                isRefreshing = isRefreshing,
                                containerColor = Color.Transparent,
                                threshold = PULL_TO_REFRESH_INDICATOR_THRESHOLD.dp,
                                elevation = 0.dp,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    YralLoader(PULL_TO_REFRESH_INDICATOR_SIZE.dp)
                }
            },
        ) {
            if (activeVideos.itemCount == 0 && activeVideos.loadState.refresh is LoadState.NotLoading) {
                if (selectedTab == ProfileTab.Drafts) {
                    DraftsEmptyStateContent(
                        offset = offset,
                        uploadVideo = uploadVideo,
                    )
                } else {
                    EmptyStateContent(
                        offset = offset,
                        isOwnProfile = isOwnProfile,
                        uploadVideo = uploadVideo,
                    )
                }
            } else {
                if (selectedTab == ProfileTab.Drafts) {
                    DraftVideoGridContent(
                        draftVideos = draftVideos,
                        offset = offset,
                        isOwnProfile = isOwnProfile,
                        openDraftVideo = openDraftVideo,
                    )
                } else {
                    VideoGridContent(
                        gridState = gridState,
                        profileVideos = profileVideos,
                        offset = offset,
                        isOwnProfile = isOwnProfile,
                        deletingVideoId = deletingVideoId,
                        openVideoReel = openVideoReel,
                        onDeleteVideo = onDeleteVideo,
                        onDownloadVideo = onDownloadVideo,
                        onViewsClick = onViewsClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    offset: Float,
    isOwnProfile: Boolean,
    uploadVideo: () -> Unit,
) {
    val titleRes =
        if (isOwnProfile) {
            Res.string.profile_empty_title
        } else {
            Res.string.profile_empty_other_title
        }
    val subtitleRes =
        if (isOwnProfile) {
            Res.string.profile_empty_subtitle
        } else {
            Res.string.profile_empty_other_subtitle
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .offset(y = (offset - PADDING_BOTTOM_ACCOUNT_INFO).dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(titleRes),
            style =
                LocalAppTopography.current.mdSemiBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(subtitleRes),
            style =
                LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
        if (isOwnProfile) {
            Spacer(modifier = Modifier.height(30.dp))
            YralGradientButton(
                modifier = Modifier.width(236.dp),
                text = stringResource(Res.string.create_ai_video),
                onClick = uploadVideo,
            )
        }
    }
}

@Composable
private fun ProfileTabBar(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    hasDrafts: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileTabItem(
            icon =
                painterResource(
                    if (selectedTab == ProfileTab.Published) {
                        Res.drawable.ic_published_selected
                    } else {
                        Res.drawable.ic_published_unselected
                    },
                ),
            isSelected = selectedTab == ProfileTab.Published,
            onClick = { onTabSelected(ProfileTab.Published) },
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.weight(1f)) {
            ProfileTabItem(
                icon =
                    painterResource(
                        if (selectedTab == ProfileTab.Drafts) {
                            Res.drawable.ic_drafts_selected
                        } else {
                            Res.drawable.ic_drafts_unselected
                        },
                    ),
                isSelected = selectedTab == ProfileTab.Drafts,
                onClick = { onTabSelected(ProfileTab.Drafts) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasDrafts && selectedTab != ProfileTab.Drafts) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = 24.dp, y = 0.dp)
                            .background(
                                color = YralColors.Pink300,
                                shape = RoundedCornerShape(12.dp),
                            ).padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.tab_new),
                        style = LocalAppTopography.current.xsBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTabItem(
    icon: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            color = YralColors.Pink300,
                            shape = RoundedCornerShape(10.dp),
                        ),
            )
        }
    }
}

@Composable
private fun DraftsEmptyStateContent(
    offset: Float,
    uploadVideo: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .offset(y = (offset - PADDING_BOTTOM_ACCOUNT_INFO).dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.drafts_empty_title),
            style = LocalAppTopography.current.mdSemiBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.drafts_empty_subtitle),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(30.dp))
        YralGradientButton(
            modifier = Modifier.width(236.dp),
            text = stringResource(Res.string.create_ai_video),
            onClick = uploadVideo,
        )
    }
}

@Composable
private fun DraftVideoGridContent(
    draftVideos: LazyPagingItems<FeedDetails>,
    offset: Float,
    isOwnProfile: Boolean,
    openDraftVideo: (FeedDetails) -> Unit,
) {
    val generatingState by VideoGenerationTracker.state.collectAsState()
    val showGeneratingCard = generatingState.isGenerating && isOwnProfile

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .offset(y = offset.dp),
    ) {
        if (showGeneratingCard) {
            item(
                key = "video_generating_card",
                contentType = "VideoGeneratingCard",
            ) {
                VideoGeneratingCard(progress = generatingState.progress)
            }
        }

        items(
            count = draftVideos.itemCount,
            key = { index -> draftVideos.peek(index)?.let { "${it.canisterID}_${it.postID}" } ?: "draft_$index" },
            contentType = { "DraftVideo" },
        ) { index ->
            val video = draftVideos[index]
            if (video != null) {
                VideoGridItem(
                    video = video,
                    isOwnProfile = true,
                    isDeleting = false,
                    isDraft = true,
                    openVideoReel = { openDraftVideo(video) },
                    onDeleteClick = {},
                    onDownloadClick = {},
                    onViewsClick = {},
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            PagingAppendIndicator(
                loadState = draftVideos.loadState.append,
                onRetry = { draftVideos.retry() },
            )
        }
    }
}

@Composable
private fun LockedProfileContent(
    onLoginClick: () -> Unit,
    isOwnProfile: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val titleRes =
            if (isOwnProfile) {
                Res.string.profile_locked_title
            } else {
                Res.string.profile_view_locked_title
            }
        val subtitleRes =
            if (isOwnProfile) {
                Res.string.profile_locked_subtitle
            } else {
                Res.string.profile_view_locked_subtitle
            }

        Text(
            text = stringResource(titleRes),
            style = LocalAppTopography.current.mdSemiBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(subtitleRes),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(30.dp))
        YralGradientButton(
            modifier = Modifier.width(236.dp),
            text = stringResource(DesignRes.string.login),
            onClick = onLoginClick,
        )
    }
}

@Composable
private fun VideoGridContent(
    gridState: LazyGridState,
    profileVideos: LazyPagingItems<FeedDetails>,
    offset: Float,
    isOwnProfile: Boolean,
    deletingVideoId: String,
    openVideoReel: (Int) -> Unit,
    onDeleteVideo: (FeedDetails) -> Unit,
    onDownloadVideo: (FeedDetails) -> Unit,
    onViewsClick: (FeedDetails) -> Unit,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .offset(y = offset.dp),
    ) {
        items(
            count = profileVideos.itemCount,
            key = profileVideos.itemKey { "${it.canisterID}_${it.postID}" },
            contentType = profileVideos.itemContentType { "ProfileVideo" },
        ) { index ->
            val video = profileVideos[index]
            if (video != null) {
                VideoGridItem(
                    video = video,
                    isOwnProfile = isOwnProfile,
                    isDeleting = deletingVideoId == video.videoID,
                    isDraft = false,
                    openVideoReel = { openVideoReel(index) },
                    onDeleteClick = { onDeleteVideo(video) },
                    onDownloadClick = { onDownloadVideo(video) },
                    onViewsClick = { onViewsClick(video) },
                )
            }
        }

        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            PagingAppendIndicator(
                loadState = profileVideos.loadState.append,
                onRetry = { profileVideos.retry() },
            )
        }
    }
}

@Composable
private fun PagingAppendIndicator(
    loadState: LoadState,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    when (loadState) {
        is LoadState.Loading -> {
            PagingLoadingIndicator(modifier = modifier)
        }

        is LoadState.Error -> {
            PagingErrorIndicator(
                modifier = modifier,
                onRetry = onRetry,
            )
        }

        is LoadState.NotLoading -> {}
    }
}

@Composable
private fun PagingLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        YralLoader(size = 24.dp)
    }
}

@Composable
private fun PagingErrorIndicator(
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onRetry != null) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = stringResource(Res.string.error_loading_more_videos),
                    style = LocalAppTopography.current.xsBold,
                    color = YralColors.NeutralTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                YralGradientButton(
                    text = stringResource(DesignRes.string.try_again),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    buttonState = YralButtonState.Enabled,
                    buttonType = YralButtonType.Transparent,
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.error_loading_more_videos),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun VideoGridItem(
    video: FeedDetails,
    isOwnProfile: Boolean,
    isDeleting: Boolean,
    isDraft: Boolean = false,
    openVideoReel: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onViewsClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(GRID_ITEM_ASPECT_RATIO)
                .clip(shape = RoundedCornerShape(8.dp))
                .background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(8.dp),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable { openVideoReel() },
        ) {
            YralAsyncImage(
                imageUrl = video.thumbnail,
                loaderSize = LoaderSize.Fixed,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                contentScale = ContentScale.Crop,
            )
            VideoGridItemActions(
                isLiked = video.isLiked,
                likeCount = video.likeCount,
                viewCount = video.bulkViewCount,
                isOwnProfile = isOwnProfile,
                isDraft = isDraft,
                onDeleteVideo = onDeleteClick,
                onDownloadVideo = onDownloadClick,
                onViewsClick = onViewsClick,
            )
        }
        DeletingOverLay(
            isDeleting = isDeleting,
        )
        DraftOverlay(
            isDraft = isDraft,
        )
    }
}

@Composable
private fun DeletingOverLay(
    isDeleting: Boolean,
    loaderSize: Dp = 24.dp,
    textStyle: TextStyle = LocalAppTopography.current.baseMedium,
) {
    AnimatedVisibility(
        visible = isDeleting,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(YralColors.ScrimColor)
                    .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.Start,
            ) {
                YralLoader(size = loaderSize, LottieRes.READ_LOADER)
                Text(
                    text = stringResource(Res.string.deleting),
                    style = textStyle,
                    color = YralColors.NeutralTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DraftOverlay(isDraft: Boolean) {
    AnimatedVisibility(
        visible = isDraft,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(YralColors.ScrimColorDraft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.draft_label),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun VideoGeneratingCard(progress: Float) {
    val percentText = "${(progress * 100).toInt()}%"
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(GRID_ITEM_ASPECT_RATIO)
                .clip(shape = RoundedCornerShape(8.dp))
                .background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(8.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                text = percentText,
                style = LocalAppTopography.current.xlBold,
                color = YralColors.NeutralTextPrimary,
            )
            Text(
                text = stringResource(Res.string.video_generating),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.Neutral300,
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(YralColors.Neutral800),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clipToBounds()
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                brush =
                                    Brush.horizontalGradient(
                                        colors =
                                            listOf(
                                                YralColors.Pink200,
                                                YralColors.Pink300,
                                            ),
                                    ),
                            ),
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun BoxScope.VideoGridItemActions(
    isLiked: Boolean,
    likeCount: ULong,
    viewCount: ULong?,
    isLikeVisible: Boolean = false,
    isOwnProfile: Boolean,
    isDraft: Boolean = false,
    onDeleteVideo: () -> Unit,
    onDownloadVideo: () -> Unit,
    onViewsClick: () -> Unit,
) {
    val leftIcon =
        if (isLikeVisible) {
            if (likeCount > 0U && isLiked) {
                Res.drawable.pink_heart
            } else {
                Res.drawable.white_heart
            }
        } else {
            DesignRes.drawable.ic_views
        }
    val leftIconDescription = if (isLikeVisible) "likes" else "views"
    val leftText = if (isLikeVisible) likeCount else viewCount
    val downloadText = stringResource(Res.string.download)
    val deleteText = stringResource(Res.string.delete)
    val menuItems =
        remember(isDraft, downloadText, deleteText) {
            buildList {
                add(
                    YralContextMenuItem(
                        text = downloadText,
                        icon = DesignRes.drawable.ic_download,
                        onClick = onDownloadVideo,
                    ),
                )
                if (!isDraft) {
                    add(
                        YralContextMenuItem(
                            text = deleteText,
                            icon = DesignRes.drawable.delete,
                            onClick = onDeleteVideo,
                        ),
                    )
                }
            }
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier.clickable {
                    if (!isLikeVisible) {
                        onViewsClick()
                    }
                },
        ) {
            Image(
                painter = painterResource(leftIcon),
                contentDescription = leftIconDescription,
                modifier = Modifier.size(24.dp),
            )
            leftText?.let {
                Text(
                    text = formatAbbreviation(it.toLong()),
                    style = LocalAppTopography.current.baseMedium,
                    color = YralColors.NeutralTextPrimary,
                )
            } ?: if (!isLikeVisible && !isDraft) {
                YralLoadingDots()
            } else {
                Unit
            }
        }
        if (isOwnProfile) {
            YralContextMenu(
                items = menuItems,
                triggerIcon = DesignRes.drawable.ic_dots_vertical,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadTriggeredSheet(
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
) {
    YralBottomSheet(
        bottomSheetState = bottomSheetState,
        onDismissRequest = onDismissRequest,
        dragHandle = { YralDragHandle() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .background(color = YralColors.Neutral900)
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 36.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.download),
                contentDescription = "Download",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(100.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(Res.string.downloading_video),
                    style = LocalAppTopography.current.lgBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(Res.string.downloading_video_desc),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralIconsActive,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DraftVideoDetailScreen(
    feedDetails: FeedDetails,
    isPublishing: Boolean,
    onBack: () -> Unit,
    onPublish: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        YralVideoPlayer(
            url = feedDetails.url,
            autoPlay = true,
            loop = true,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "back",
            tint = Color.White,
            modifier =
                Modifier
                    .padding(16.dp)
                    .size(24.dp)
                    .align(Alignment.TopStart)
                    .clickable { onBack() },
        )
        YralGradientButton(
            text = stringResource(Res.string.publish_button),
            onClick = onPublish,
            buttonState = if (isPublishing) YralButtonState.Loading else YralButtonState.Enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun BecomeProButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .border(width = 1.dp, color = YralColors.Yellow200, shape = RoundedCornerShape(size = 4.dp))
                .background(color = YralColors.Yellow400, shape = RoundedCornerShape(size = 4.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.become_pro),
            style = LocalAppTopography.current.regSemiBold,
            color = YralColors.Yellow200,
        )
        Image(
            painter = painterResource(DesignRes.drawable.ic_thunder),
            contentDescription = "Pro",
            contentScale = ContentScale.Inside,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun DraftsEmptyStatePreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        Box(
            modifier =
                Modifier
                    .background(YralColors.Neutral900)
                    .fillMaxSize(),
        ) {
            DraftsEmptyStateContent(
                offset = 0f,
                uploadVideo = {},
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun BecomeProButtonPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        Box(
            modifier =
                Modifier
                    .background(YralColors.Neutral900)
                    .padding(24.dp),
        ) {
            BecomeProButton(onClick = {})
        }
    }
}
