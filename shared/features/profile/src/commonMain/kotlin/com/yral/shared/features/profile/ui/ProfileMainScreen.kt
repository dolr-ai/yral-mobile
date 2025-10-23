package com.yral.shared.features.profile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.profile.viewmodel.DeleteConfirmationState
import com.yral.shared.features.profile.viewmodel.ProfileBottomSheet
import com.yral.shared.features.profile.viewmodel.ProfileEvents
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.features.profile.viewmodel.VideoViewState
import com.yral.shared.features.profile.viewmodel.ViewState
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.AccountInfoView
import com.yral.shared.libs.designsystem.component.DeleteConfirmationSheet
import com.yral.shared.libs.designsystem.component.LoaderSize
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showError
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import com.yral.shared.rust.service.domain.models.FollowerItem
import com.yral.shared.rust.service.domain.models.PagedFollowerItem
import com.yral.shared.rust.service.utils.propicFromPrincipal
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.profile.generated.resources.Res
import yral_mobile.shared.features.profile.generated.resources.clapperboard
import yral_mobile.shared.features.profile.generated.resources.delete
import yral_mobile.shared.features.profile.generated.resources.delete_video
import yral_mobile.shared.features.profile.generated.resources.deleting
import yral_mobile.shared.features.profile.generated.resources.error_loading_more_videos
import yral_mobile.shared.features.profile.generated.resources.error_loading_videos
import yral_mobile.shared.features.profile.generated.resources.failed_to_delete_video
import yral_mobile.shared.features.profile.generated.resources.followers_empty_list
import yral_mobile.shared.features.profile.generated.resources.followers_load_error
import yral_mobile.shared.features.profile.generated.resources.following_empty_list
import yral_mobile.shared.features.profile.generated.resources.pink_heart
import yral_mobile.shared.features.profile.generated.resources.video_will_be_deleted_permanently
import yral_mobile.shared.features.profile.generated.resources.white_heart
import yral_mobile.shared.features.profile.generated.resources.you_have_not_uploaded_any_video_yet
import yral_mobile.shared.libs.designsystem.generated.resources.account_nav
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.cancel
import yral_mobile.shared.libs.designsystem.generated.resources.delete
import yral_mobile.shared.libs.designsystem.generated.resources.follow
import yral_mobile.shared.libs.designsystem.generated.resources.followers
import yral_mobile.shared.libs.designsystem.generated.resources.following
import yral_mobile.shared.libs.designsystem.generated.resources.ic_views
import yral_mobile.shared.libs.designsystem.generated.resources.login_to_follow_subtext
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.my_profile
import yral_mobile.shared.libs.designsystem.generated.resources.oops
import yral_mobile.shared.libs.designsystem.generated.resources.something_went_wrong
import yral_mobile.shared.libs.designsystem.generated.resources.started_following
import yral_mobile.shared.libs.designsystem.generated.resources.try_again
import yral_mobile.shared.libs.designsystem.generated.resources.upload_video
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

internal const val GRID_ITEM_ASPECT_RATIO = 0.75f
internal const val PULL_TO_REFRESH_INDICATOR_SIZE = 34f
internal const val PULL_TO_REFRESH_INDICATOR_THRESHOLD = 36f
internal const val PULL_TO_REFRESH_OFFSET_MULTIPLIER = 1.5f
internal const val MAX_LINES_FOR_POST_DESCRIPTION = 5
internal const val PADDING_BOTTOM_ACCOUNT_INFO = 20

private fun calculateSheetFraction(itemCount: Int): Float {
    val base = FollowersSheetUi.MIN_HEIGHT_FRACTION
    val increment = 0.05f
    return (base + itemCount * increment).coerceIn(base, FollowersSheetUi.EXPANDED_HEIGHT_FRACTION)
}

private fun LazyPagingItems<PagedFollowerItem>.snapshotCount(): Int = itemSnapshotList.items.sumOf { it.items.size }

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ProfileMainScreen(
    component: ProfileMainComponent,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    profileVideos: LazyPagingItems<FeedDetails>,
    loginState: UiState<*>,
    loginBottomSheet: LoginBottomSheetComposable,
    getPrefetchListener: (reel: Reels) -> PrefetchVideoListener,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val followers = viewModel.followers.collectAsLazyPagingItems()
    val following = viewModel.following.collectAsLazyPagingItems()

    val followedSuccessfully = stringResource(DesignRes.string.started_following, state.accountInfo?.displayName ?: "")
    LaunchedEffect(Unit) {
        viewModel.profileEvents.collect { event ->
            when (event) {
                is ProfileEvents.FollowedSuccessfully -> {
                    ToastManager.showSuccess(type = ToastType.Small(message = followedSuccessfully))
                    followers.refresh()
                    following.refresh()
                }
                is ProfileEvents.UnfollowedSuccessfully -> {
                    followers.refresh()
                    following.refresh()
                }
                is ProfileEvents.Failed -> {
                    ToastManager.showError(type = ToastType.Small(message = event.message))
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

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val followersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var followerSheetTab by remember { mutableStateOf<FollowersSheetTab?>(null) }
    LaunchedEffect(loginState) {
        if (loginState is UiState.Failure) {
            viewModel.setBottomSheetType(ProfileBottomSheet.SignUp)
        }
    }

    val backHandlerEnabled by remember(state.videoView) {
        mutableStateOf(state.videoView is VideoViewState.ViewingReels)
    }
    LaunchedEffect(Unit) { viewModel.pushScreenView(profileVideos.itemCount) }

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
        onBack = { viewModel.closeVideoReel() },
    )

    val deletingVideoId =
        remember(state.deleteConfirmation) {
            when (val deleteConfirmation = state.deleteConfirmation) {
                is DeleteConfirmationState.InProgress -> deleteConfirmation.request.feedDetails.videoID
                else -> ""
            }
        }

    val gridState = rememberLazyGridState()
    Box(modifier = modifier.fillMaxSize()) {
        when (val videoViewState = state.videoView) {
            is VideoViewState.ViewingReels -> {
                if (profileVideos.itemCount > 0) {
                    val msgFeedVideoShare = stringResource(DesignRes.string.msg_feed_video_share)
                    val msgFeedVideoShareDesc = stringResource(DesignRes.string.msg_feed_video_share_desc)
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
                        onShareClick = { feedDetails ->
                            viewModel.onShareClicked(
                                feedDetails,
                                msgFeedVideoShare,
                                msgFeedVideoShareDesc,
                            )
                        },
                        getPrefetchListener = getPrefetchListener,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    viewModel.closeVideoReel()
                }
            }
            VideoViewState.None -> {
                MainContent(
                    modifier = Modifier.fillMaxSize(),
                    bottomSheetState = bottomSheetState,
                    state = state,
                    viewModel = viewModel,
                    gridState = gridState,
                    profileVideos = profileVideos,
                    followers = followers,
                    following = following,
                    deletingVideoId = deletingVideoId,
                    loginBottomSheet = loginBottomSheet,
                    callbacks =
                        MainContentCallbacks(
                            uploadVideo = {
                                viewModel.uploadVideoClicked()
                                component.onUploadVideoClick()
                            },
                            openAccount = { component.openAccount() },
                            openEditProfile = { component.openEditProfile() },
                            onBackClicked = { component.onBackClicked() },
                            onFollowersSectionClick = { followerSheetTab = it },
                        ),
                )
            }
        }
    }

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

    val accountInfo = state.accountInfo
    if (followerSheetTab != null && accountInfo != null) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val maxSheetHeight = screenHeight * FollowersSheetUi.EXPANDED_HEIGHT_FRACTION
        val currentListSize =
            when (followerSheetTab!!) {
                FollowersSheetTab.Followers -> followers.snapshotCount()
                FollowersSheetTab.Following -> following.snapshotCount()
            }
        val minSheetHeight =
            (screenHeight * calculateSheetFraction(currentListSize)).coerceAtMost(maxSheetHeight)
        LaunchedEffect(followerSheetTab) {
            if (followerSheetTab != null) {
                if (!followersSheetState.isVisible) {
                    followersSheetState.show()
                }
                if (followersSheetState.hasPartiallyExpandedState &&
                    followersSheetState.currentValue != SheetValue.PartiallyExpanded
                ) {
                    followersSheetState.partialExpand()
                }
            }
        }
        ModalBottomSheet(
            sheetState = followersSheetState,
            onDismissRequest = { followerSheetTab = null },
            containerColor = YralColors.Neutral900,
            modifier =
                Modifier.heightIn(
                    min = minSheetHeight,
                    max = maxSheetHeight,
                ),
            dragHandle = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = FollowersSheetUi.DragHandleTopSpacing),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .width(FollowersSheetUi.DragHandleWidth)
                                .height(FollowersSheetUi.DragHandleHeight)
                                .clip(RoundedCornerShape(FollowersSheetUi.DragHandleCornerRadius))
                                .background(YralColors.Neutral500),
                    )
                }
            },
        ) {
            FollowersBottomSheetContent(
                username = accountInfo.displayName,
                initialTab = followerSheetTab!!,
                followers = followers,
                following = following,
                minSheetHeight = minSheetHeight,
                maxSheetHeight = maxSheetHeight,
                onTabSelected = { followerSheetTab = it },
                onFollowToggle = viewModel::toggleFollowForPrincipal,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
typealias LoginBottomSheetComposable = @Composable (
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    termsLink: String,
    openTerms: () -> Unit,
) -> Unit

@Suppress("LongMethod", "CyclomaticComplexMethod")
private data class MainContentCallbacks(
    val uploadVideo: () -> Unit,
    val openAccount: () -> Unit,
    val openEditProfile: () -> Unit,
    val onBackClicked: () -> Unit,
    val onFollowersSectionClick: (FollowersSheetTab) -> Unit,
)

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    modifier: Modifier,
    bottomSheetState: SheetState,
    state: ViewState,
    viewModel: ProfileViewModel,
    gridState: LazyGridState,
    profileVideos: LazyPagingItems<FeedDetails>,
    followers: LazyPagingItems<PagedFollowerItem>?,
    following: LazyPagingItems<PagedFollowerItem>?,
    deletingVideoId: String,
    loginBottomSheet: LoginBottomSheetComposable,
    callbacks: MainContentCallbacks,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ProfileHeader(
            isOwnProfile = state.isOwnProfile,
            userName = state.accountInfo?.displayName,
            isWalletEnabled = state.isWalletEnabled,
            openAccount = callbacks.openAccount,
            onBack = callbacks.onBackClicked,
        )
        state.accountInfo?.let { info ->
            val followersCount =
                followers?.let { pagingItems ->
                    if (pagingItems.itemCount > 0) {
                        pagingItems[0]?.totalCount?.toLong() ?: 0
                    } else {
                        0
                    }
                } ?: 0
            val followingCount =
                following?.let { pagingItems ->
                    if (pagingItems.itemCount > 0) {
                        pagingItems[0]?.totalCount?.toLong() ?: 0
                    } else {
                        0
                    }
                } ?: 0
            AccountInfoView(
                accountInfo = info,
                totalFollowers = followersCount,
                totalFollowing = followingCount,
                isSocialSignIn = state.isLoggedIn,
                loginSubText = stringResource(DesignRes.string.login_to_follow_subtext),
                onLoginClicked = { viewModel.setBottomSheetType(ProfileBottomSheet.SignUp) },
                showEditProfile = state.isOwnProfile,
                onEditProfileClicked = callbacks.openEditProfile,
                showFollow = !state.isOwnProfile,
                isFollowing = state.isFollowing,
                isFollowInProgress = state.isFollowInProgress,
                onFollowClicked = { viewModel.followUnfollow() },
                onFollowersClick = { callbacks.onFollowersSectionClick(FollowersSheetTab.Followers) },
                onFollowingClick = { callbacks.onFollowersSectionClick(FollowersSheetTab.Following) },
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
                if (!state.isOwnProfile && !state.isLoggedIn) {
                    // Implement blocked ui profile view when needed
                } else {
                    if (state.manualRefreshTriggered) {
                        viewModel.pushScreenView(profileVideos.itemCount)
                        viewModel.setManualRefreshTriggered(false)
                    }
                    SuccessContent(
                        gridState = gridState,
                        profileVideos = profileVideos,
                        isOwnProfile = state.isOwnProfile,
                        deletingVideoId = deletingVideoId,
                        uploadVideo = callbacks.uploadVideo,
                        openVideoReel = { viewModel.openVideoReel(it) },
                        onDeleteVideo = {
                            viewModel.confirmDelete(
                                feedDetails = it,
                                ctaType = VideoDeleteCTA.PROFILE_THUMBNAIL,
                            )
                        },
                        onManualRefreshTriggered = { viewModel.setManualRefreshTriggered(it) },
                    )
                }
            }
        }
    }
    val extraSheetState = rememberModalBottomSheetState()
    var extraSheetLink by remember { mutableStateOf("") }
    when (state.bottomSheet) {
        ProfileBottomSheet.None -> Unit
        ProfileBottomSheet.SignUp -> {
            loginBottomSheet(
                bottomSheetState,
                { viewModel.setBottomSheetType(ProfileBottomSheet.None) },
                viewModel.getTncLink(),
                { extraSheetLink = viewModel.getTncLink() },
            )
        }
    }
    if (extraSheetLink.isNotEmpty()) {
        YralWebViewBottomSheet(
            link = extraSheetLink,
            bottomSheetState = extraSheetState,
            onDismissRequest = { extraSheetLink = "" },
        )
    }
}

@Composable
private fun ProfileHeader(
    isOwnProfile: Boolean,
    userName: String?,
    isWalletEnabled: Boolean,
    openAccount: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isOwnProfile) {
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
            Text(
                text =
                    if (isOwnProfile) {
                        stringResource(DesignRes.string.my_profile)
                    } else {
                        userName ?: ""
                    },
                style = LocalAppTopography.current.xlBold,
                color = YralColors.NeutralTextPrimary,
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
        horizontalAlignment = Alignment.Start,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    gridState: LazyGridState,
    profileVideos: LazyPagingItems<FeedDetails>,
    isOwnProfile: Boolean,
    deletingVideoId: String,
    uploadVideo: () -> Unit,
    openVideoReel: (Int) -> Unit,
    onDeleteVideo: (FeedDetails) -> Unit,
    onManualRefreshTriggered: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = PADDING_BOTTOM_ACCOUNT_INFO.dp),
    ) {
        val pullRefreshState = rememberPullToRefreshState()
        val offset =
            pullRefreshState.distanceFraction *
                PULL_TO_REFRESH_INDICATOR_SIZE *
                PULL_TO_REFRESH_OFFSET_MULTIPLIER

        val isRefreshing = profileVideos.loadState.refresh is LoadState.Loading

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                onManualRefreshTriggered(true)
                profileVideos.refresh()
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
            if (profileVideos.itemCount == 0 && profileVideos.loadState.refresh is LoadState.NotLoading) {
                EmptyStateContent(offset, uploadVideo)
            } else {
                VideoGridContent(
                    gridState = gridState,
                    profileVideos = profileVideos,
                    offset = offset,
                    isOwnProfile = isOwnProfile,
                    deletingVideoId = deletingVideoId,
                    openVideoReel = openVideoReel,
                    onDeleteVideo = onDeleteVideo,
                )
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
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
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.clapperboard),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.you_have_not_uploaded_any_video_yet),
            style = LocalAppTopography.current.lgMedium,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(36.dp))
        YralGradientButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(DesignRes.string.upload_video),
            buttonType = YralButtonType.White,
            onClick = uploadVideo,
        )
    }
}

private enum class FollowersSheetTab { Followers, Following }

private object FollowersSheetUi {
    const val MIN_HEIGHT_FRACTION = 0.35f
    const val EXPANDED_HEIGHT_FRACTION = 0.9f
    const val TAB_UNSELECTED_ALPHA = 0.6f
    val DragHandleTopSpacing = 12.dp
    val DragHandleWidth = 32.dp
    val DragHandleHeight = 2.dp
    val DragHandleCornerRadius = 50.dp
    val HorizontalPadding = 16.dp
    val NameTopSpacing = 28.dp
    val TabsTopSpacing = 28.dp
    val SeparatorTopSpacing = 12.dp
    val SeparatorHeight = 1.dp
    val ListTopSpacing = 36.dp
    val ListItemSpacing = 16.dp
    val AvatarSize = 42.dp
    val AvatarInitialFontSize = 16.sp
    val ItemPaddingHorizontal = 16.dp
    val ItemPaddingVertical = 12.dp
    val ActionButtonWidth = 118.dp
    val ActionButtonHeight = 36.dp
    val RowCornerRadius = 12.dp
}

@Suppress("LongMethod")
@Composable
private fun FollowersBottomSheetContent(
    username: String,
    initialTab: FollowersSheetTab,
    followers: LazyPagingItems<PagedFollowerItem>,
    following: LazyPagingItems<PagedFollowerItem>,
    minSheetHeight: Dp,
    maxSheetHeight: Dp,
    onTabSelected: (FollowersSheetTab) -> Unit,
    onFollowToggle: (String, Boolean) -> Unit,
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val pagingItems = if (selectedTab == FollowersSheetTab.Followers) followers else following

    LaunchedEffect(selectedTab) {
        if (selectedTab == FollowersSheetTab.Followers) {
            followers.refresh()
        } else {
            following.refresh()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .heightIn(min = minSheetHeight, max = maxSheetHeight)
                .padding(horizontal = FollowersSheetUi.HorizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(modifier = Modifier.height(FollowersSheetUi.NameTopSpacing))
        Text(
            text = username,
            style = LocalAppTopography.current.lgBold.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = YralColors.NeutralTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(FollowersSheetUi.TabsTopSpacing))
        FollowersTabRow(
            selectedTab = selectedTab,
            onTabSelected = {
                selectedTab = it
                onTabSelected(it)
            },
        )
        Spacer(modifier = Modifier.height(FollowersSheetUi.ListTopSpacing))
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(FollowersSheetUi.ListItemSpacing),
        ) {
            when (val refreshState = pagingItems.loadState.refresh) {
                is LoadState.Loading -> {
                    item { FollowersLoadingState() }
                }

                is LoadState.Error -> {
                    item {
                        FollowersErrorState(onRetry = { pagingItems.retry() })
                    }
                }

                is LoadState.NotLoading -> {
                    if (pagingItems.itemCount == 0) {
                        item { FollowersEmptyState(selectedTab) }
                    } else {
                        for (pageIndex in 0 until pagingItems.itemCount) {
                            val page = pagingItems[pageIndex]
                            if (page != null) {
                                items(
                                    page.items,
                                    key = { follower -> toReadablePrincipalText(follower.principalId.toString()) + "-$pageIndex" }) {
                                    follower ->
                                        FollowerRow(
                                            follower = follower,
                                            onFollowToggle = onFollowToggle,
                                        )
                                }
                            }
                        }
                    }
                }
            }

            item {
                PagingAppendIndicator(
                    loadState = pagingItems.loadState.append,
                    onRetry = pagingItems::retry,
                )
            }
        }
    }
}

@Composable
private fun FollowersLoadingState() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        YralLoader(size = 32.dp)
    }
}

@Composable
private fun FollowersErrorState(onRetry: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.followers_load_error),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        YralGradientButton(
            modifier = Modifier.width(160.dp),
            text = stringResource(DesignRes.string.try_again),
            buttonHeight = 36.dp,
            fillMaxWidth = false,
            onClick = onRetry,
        )
    }
}

@Composable
private fun FollowersEmptyState(tab: FollowersSheetTab) {
    val message =
        when (tab) {
            FollowersSheetTab.Followers -> stringResource(Res.string.followers_empty_list)
            FollowersSheetTab.Following -> stringResource(Res.string.following_empty_list)
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FollowersTabRow(
    selectedTab: FollowersSheetTab,
    onTabSelected: (FollowersSheetTab) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FollowersSheetUi.SeparatorTopSpacing),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            FollowersTabItem(
                modifier = Modifier.align(Alignment.CenterStart),
                text = stringResource(DesignRes.string.followers),
                isSelected = selectedTab == FollowersSheetTab.Followers,
                onClick = { onTabSelected(FollowersSheetTab.Followers) },
            )
            FollowersTabItem(
                modifier = Modifier.align(Alignment.CenterEnd),
                text = stringResource(DesignRes.string.following),
                isSelected = selectedTab == FollowersSheetTab.Following,
                onClick = { onTabSelected(FollowersSheetTab.Following) },
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(FollowersSheetUi.SeparatorHeight)
                    .background(YralColors.Neutral700),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(FollowersSheetUi.SeparatorHeight)
                        .align(
                            if (selectedTab == FollowersSheetTab.Followers) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            },
                        ).background(YralColors.Pink300),
            )
        }
    }
}

@Composable
private fun FollowersTabItem(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(0.5f)
                .then(modifier)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.baseRegular.copy(fontSize = 14.sp),
            color =
                if (isSelected) {
                    YralColors.NeutralTextPrimary
                } else {
                    YralColors.NeutralTextPrimary.copy(alpha = FollowersSheetUi.TAB_UNSELECTED_ALPHA)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun FollowerRow(
    follower: FollowerItem,
    onFollowToggle: (String, Boolean) -> Unit,
) {
    val principalText = remember(follower.principalId) { toReadablePrincipalText(follower.principalId.toString()) }
    val displayName = remember(principalText) { resolveUsername(null, principalText) ?: principalText }
    val initial = remember(displayName) { displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }
    val isFollowing = follower.callerFollows
    val avatarUrl =
        remember(follower.profilePictureUrl, principalText) {
            follower.profilePictureUrl?.takeIf { it.isNotBlank() } ?: propicFromPrincipal(principalText)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FollowersSheetUi.RowCornerRadius))
                .background(YralColors.Neutral900)
                .padding(vertical = FollowersSheetUi.ItemPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FollowersSheetUi.ItemPaddingVertical),
        ) {
            YralAsyncImage(
                imageUrl = avatarUrl,
                modifier =
                    Modifier
                        .size(FollowersSheetUi.AvatarSize)
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = displayName,
                style = LocalAppTopography.current.baseSemiBold.copy(fontSize = 14.sp),
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isFollowing) {
            YralButton(
                text = stringResource(DesignRes.string.following),
                modifier =
                    Modifier
                        .width(FollowersSheetUi.ActionButtonWidth)
                        .height(FollowersSheetUi.ActionButtonHeight),
                backgroundColor = YralColors.Neutral800,
                borderColor = YralColors.Neutral700,
                textStyle = LocalAppTopography.current.baseSemiBold.copy(fontSize = 14.sp, color = YralColors.Neutral50),
                buttonHeight = FollowersSheetUi.ActionButtonHeight,
                fillMaxWidth = false,
                onClick = { onFollowToggle(principalText, true) },
            )
        } else {
            YralGradientButton(
                modifier =
                    Modifier
                        .width(FollowersSheetUi.ActionButtonWidth)
                        .height(FollowersSheetUi.ActionButtonHeight),
                buttonHeight = FollowersSheetUi.ActionButtonHeight,
                fillMaxWidth = false,
                text = stringResource(DesignRes.string.follow),
                onClick = { onFollowToggle(principalText, false) },
            )
        }
    }
}

private fun toReadablePrincipalText(raw: String): String =
    raw
        .removePrefix("Principal(")
        .removeSuffix(")")
        .removePrefix("text=")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoGridContent(
    gridState: LazyGridState,
    profileVideos: LazyPagingItems<FeedDetails>,
    offset: Float,
    isOwnProfile: Boolean,
    deletingVideoId: String,
    openVideoReel: (Int) -> Unit,
    onDeleteVideo: (FeedDetails) -> Unit,
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
            key = profileVideos.itemKey { it.videoID },
            contentType = profileVideos.itemContentType { "ProfileVideo" },
        ) { index ->
            val video = profileVideos[index]
            if (video != null) {
                VideoGridItem(
                    video = video,
                    isOwnProfile = isOwnProfile,
                    isDeleting = deletingVideoId == video.videoID,
                    openVideoReel = { openVideoReel(index) },
                    onDeleteClick = { onDeleteVideo(video) },
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
    openVideoReel: () -> Unit,
    onDeleteClick: () -> Unit,
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
                viewCount = video.viewCount,
                isOwnProfile = isOwnProfile,
                onDeleteVideo = onDeleteClick,
            )
        }
        DeletingOverLay(
            isDeleting = isDeleting,
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
                )
            }
        }
    }
}

@Composable
private fun BoxScope.VideoGridItemActions(
    isLiked: Boolean,
    likeCount: ULong,
    viewCount: ULong,
    isLikeVisible: Boolean = false,
    isOwnProfile: Boolean,
    onDeleteVideo: () -> Unit,
) {
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
        ) {
            val icon =
                if (isLikeVisible) {
                    if (likeCount > 0U && isLiked) {
                        Res.drawable.pink_heart
                    } else {
                        Res.drawable.white_heart
                    }
                } else {
                    DesignRes.drawable.ic_views
                }
            val iconDescription =
                if (isLikeVisible) {
                    "likes"
                } else {
                    "views"
                }
            val iconCount =
                if (isLikeVisible) {
                    likeCount
                } else {
                    viewCount
                }
            Image(
                painter = painterResource(icon),
                contentDescription = iconDescription,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = formatAbbreviation(iconCount.toLong()),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextPrimary,
            )
        }
        if (isOwnProfile) {
            Image(
                painter = painterResource(DesignRes.drawable.delete),
                contentDescription = "Delete video",
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable { onDeleteVideo() },
            )
        }
    }
}
