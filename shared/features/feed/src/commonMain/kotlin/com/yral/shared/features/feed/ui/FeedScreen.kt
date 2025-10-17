package com.yral.shared.features.feed.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.components.SignupNudge
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.SIGN_UP_PAGE
import com.yral.shared.features.feed.viewmodel.OverlayType
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import com.yral.shared.libs.videoPlayer.util.ReelScrollDirection
import com.yral.shared.reportVideo.domain.models.ReportSheetState
import com.yral.shared.reportVideo.ui.ReportVideo
import com.yral.shared.reportVideo.ui.ReportVideoSheet
import com.yral.shared.rust.service.domain.models.toCanisterData
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login_desc
import yral_mobile.shared.libs.designsystem.generated.resources.ic_follow
import yral_mobile.shared.libs.designsystem.generated.resources.ic_following
import yral_mobile.shared.libs.designsystem.generated.resources.ic_share
import yral_mobile.shared.libs.designsystem.generated.resources.ic_views
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.ok
import yral_mobile.shared.libs.designsystem.generated.resources.shadow_bottom
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun FeedScreen(
    component: FeedComponent,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
    topOverlay: @Composable (pageNo: Int) -> Unit,
    bottomOverlay: @Composable (pageNo: Int) -> Unit,
    onPageChanged: (pageNo: Int, currentPage: Int) -> Unit,
    onEdgeScrollAttempt: (pageNo: Int) -> Unit,
    limitReelCount: Int,
    getPrefetchListener: (reel: Reels) -> PrefetchVideoListener,
    getVideoListener: (reel: Reels) -> VideoListener?,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.pushScreenView() }

    // Observe deep link navigation reactively - works even if screen is already open
    LaunchedEffect(Unit) {
        component.openPostDetails.collectLatest {
            if (it != null) {
                viewModel.showDeeplinkedVideoFirst(it.postId, it.canisterId)
            }
        }
    }

    // Set initial video ID when feed loads
    LaunchedEffect(state.feedDetails.isNotEmpty()) {
        if (state.currentPageOfFeed < state.feedDetails.size) {
            onPageChanged(state.currentPageOfFeed, state.currentPageOfFeed)
        }
    }

    // Pagination logic
    val isNearEnd by remember {
        derivedStateOf {
            state.feedDetails.isNotEmpty() &&
                (state.feedDetails.size - state.currentPageOfFeed) <= PRE_FETCH_BEFORE_LAST
        }
    }
    LaunchedEffect(isNearEnd, state.isLoadingMore, state.pendingFetchDetails) {
        if (isNearEnd && !state.isLoadingMore && state.pendingFetchDetails == 0) {
            Logger.d("FeedPagination") { "triggering pagination" }
            viewModel.loadMoreFeed()
        }
    }

    // Determine if we should show the loader
    val showLoader =
        isNearEnd &&
            (state.isLoadingMore || state.pendingFetchDetails > 0) &&
            state.currentPageOfFeed == state.feedDetails.size - 1

    // Cold-start deeplink overlay: block UI with a centered loader until deeplinked item is fetched
    if (state.isDeeplinkFetching) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            YralLoader(size = 48.dp)
        }
        return
    }

    Column(modifier = modifier) {
        if (state.feedDetails.isNotEmpty()) {
            YRALReelPlayer(
                modifier = Modifier.weight(1f),
                reels = getReels(state),
                maxReelsInPager = limitReelCount,
                initialPage = state.currentPageOfFeed,
                onPageLoaded = { page ->
                    // call onPageChanged before changing page in FeedViewModel
                    onPageChanged(page, state.currentPageOfFeed)
                    viewModel.onCurrentPageChange(page)
                    viewModel.setPostDescriptionExpanded(false)
                },
                recordTime = { currentTime, totalTime ->
                    viewModel.recordTime(currentTime, totalTime)
                },
                didVideoEnd = { viewModel.didCurrentVideoEnd() },
                getPrefetchListener = getPrefetchListener,
                getVideoListener = getVideoListener,
                onEdgeScrollAttempt = { page, atFirst, direction ->
                    if (!atFirst && direction == ReelScrollDirection.Up) {
                        onEdgeScrollAttempt(page)
                    }
                },
            ) { pageNo ->
                FeedOverlay(
                    pageNo = pageNo,
                    state = state,
                    feedViewModel = viewModel,
                    topOverlay = { topOverlay(pageNo) },
                    bottomOverlay = { bottomOverlay(pageNo) },
                    openProfile = { canisterData -> component.openProfile(canisterData) },
                )
            }
            // Show loader at the bottom when loading more content AND no new items have been added yet
            if (showLoader) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    YralLoader(size = 20.dp)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                YralLoader()
            }
        }
    }

    if (state.reportSheetState is ReportSheetState.Open) {
        ReportVideoSheet(
            bottomSheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                ),
            onDismissRequest = { viewModel.toggleReportSheet(false, 0) },
            isLoading = state.isReporting,
            reasons = (state.reportSheetState as ReportSheetState.Open).reasons,
            onSubmit = { reportVideoData ->
                viewModel.reportVideo(
                    pageNo = (state.reportSheetState as ReportSheetState.Open).pageNo,
                    reportVideoData = reportVideoData,
                )
            },
        )
    }

    if (state.showSignupFailedSheet) {
        YralErrorMessage(
            title = stringResource(DesignRes.string.could_not_login),
            error = stringResource(DesignRes.string.could_not_login_desc),
            sheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                ),
            cta = stringResource(DesignRes.string.ok),
            onClick = { viewModel.toggleSignupFailed(false) },
            onDismiss = { viewModel.toggleSignupFailed(false) },
        )
    }
}

private fun getReels(state: FeedState): List<Reels> =
    state
        .feedDetails
        .map { it.toReel() }
        .toList()

private fun FeedDetails.toReel() =
    Reels(
        videoUrl = url,
        thumbnailUrl = thumbnail,
        videoId = videoID,
    )

@Composable
private fun FeedOverlay(
    pageNo: Int,
    state: FeedState,
    feedViewModel: FeedViewModel,
    topOverlay: @Composable () -> Unit,
    bottomOverlay: @Composable () -> Unit,
    openProfile: (userCanisterData: CanisterData) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        topOverlay()
        BottomView(
            state = state,
            pageNo = pageNo,
            feedViewModel = feedViewModel,
            bottomOverlay = bottomOverlay,
            openProfile = openProfile,
        )
        if (!state.isLoggedIn && pageNo != 0 && (pageNo % SIGN_UP_PAGE) == 0) {
            val context = getContext()
            SignupNudge(tncLink = feedViewModel.getTncLink()) {
                feedViewModel.signInWithGoogle(context)
            }
        }
    }
}

@Composable
private fun BottomView(
    state: FeedState,
    pageNo: Int,
    feedViewModel: FeedViewModel,
    bottomOverlay: @Composable () -> Unit,
    openProfile: (userCanisterData: CanisterData) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Shadow(Modifier.align(Alignment.BottomCenter))
        ActionsRight(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 105.dp),
            pageNo = pageNo,
            state = state,
            feedViewModel = feedViewModel,
            openProfile = openProfile,
        )
        bottomOverlay()
    }
}

@Composable
private fun Shadow(modifier: Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(DesignRes.drawable.shadow_bottom),
                    contentScale = ContentScale.FillBounds,
                    alpha = 0.3f,
                ),
    )
}

@Suppress("LongMethod")
@Composable
private fun ActionsRight(
    modifier: Modifier,
    pageNo: Int,
    state: FeedState,
    feedViewModel: FeedViewModel,
    openProfile: (userCanisterData: CanisterData) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val feedDetails = state.feedDetails[pageNo]
        if (state.overlayType in listOf(OverlayType.GAME_TOGGLE, OverlayType.DAILY_RANK)) {
            feedDetails.profileImageURL?.let { profileImage ->
                Column(
                    modifier =
                        Modifier
                            .offset(y = 16.dp)
                            .clickable { openProfile(feedDetails.toCanisterData()) },
                ) {
                    YralAsyncImage(
                        imageUrl = profileImage,
                        border = 2.dp,
                        borderColor = Color.White,
                        backgroundColor = YralColors.ProfilePicBackground,
                        modifier = Modifier.size(36.dp),
                    )
                    Image(
                        painter =
                            painterResource(
                                resource =
                                    if (feedDetails.isFollowing) {
                                        DesignRes.drawable.ic_following
                                    } else {
                                        DesignRes.drawable.ic_follow
                                    },
                            ),
                        contentDescription = "follow",
                        contentScale = ContentScale.None,
                        modifier = Modifier.size(36.dp).offset(y = (-10).dp),
                    )
                }
            }
        }
        val msgFeedVideoShare = stringResource(DesignRes.string.msg_feed_video_share)
        val msgFeedVideoShareDesc = stringResource(DesignRes.string.msg_feed_video_share_desc)
        Image(
            modifier =
                Modifier
                    .size(36.dp)
                    .padding(1.5.dp)
                    .clickable { feedViewModel.onShareClicked(feedDetails, msgFeedVideoShare, msgFeedVideoShareDesc) },
            painter = painterResource(DesignRes.drawable.ic_share),
            contentDescription = "share video",
            contentScale = ContentScale.None,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(DesignRes.drawable.ic_views),
                contentDescription = "video views",
                contentScale = ContentScale.None,
                modifier =
                    Modifier
                        .size(36.dp)
                        .padding(1.5.dp),
            )
            Text(
                text = formatAbbreviation(feedDetails.viewCount.toLong(), 1),
                style = LocalAppTopography.current.regSemiBold,
                color = YralColors.NeutralTextPrimary,
            )
        }

        ReportVideo(
            onReportClicked = { feedViewModel.toggleReportSheet(true, pageNo) },
        )
    }
}

@Composable
internal expect fun getContext(): Any
