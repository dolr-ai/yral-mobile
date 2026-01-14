package com.yral.shared.features.feed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.auth.ui.LoginMode
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.auth.ui.rememberLoginInfo
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.viewmodel.FeedEvents
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.FOLLOW_NUDGE_PAGE
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.SIGN_UP_PAGE
import com.yral.shared.features.feed.viewmodel.OnboardingStep
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showError
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.libs.videoPlayer.YRALReelPlayerCardStack
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import com.yral.shared.libs.videoPlayer.util.ReelScrollDirection
import com.yral.shared.reportVideo.domain.models.ReportSheetState
import com.yral.shared.reportVideo.ui.ReportVideoSheet
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.feed.generated.resources.Res
import yral_mobile.shared.features.feed.generated.resources.scroll_to_next_video
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login_desc
import yral_mobile.shared.libs.designsystem.generated.resources.ok
import yral_mobile.shared.libs.designsystem.generated.resources.shadow_bottom
import yral_mobile.shared.libs.designsystem.generated.resources.started_following
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun FeedScreen(
    component: FeedComponent,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
    topOverlay: @Composable (pageNo: Int) -> Unit,
    actionsRight: @Composable ColumnScope.(pageNo: Int) -> Unit,
    bottomOverlay: @Composable (pageNo: Int, scrollToNext: () -> Unit) -> Unit,
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
                viewModel.showDeeplinkedVideoFirst(
                    postId = it.postId,
                    canisterId = it.canisterId,
                    publisherUserId = it.publisherUserId,
                )
                if (!state.isLoggedIn) {
                    component.promptLogin(it)
                }
            }
        }
    }

    // Set initial video ID when feed loads
    LaunchedEffect(state.feedDetails.isNotEmpty()) {
        if (state.currentPageOfFeed < state.feedDetails.size) {
            onPageChanged(state.currentPageOfFeed, state.currentPageOfFeed)
        }
        viewModel.consumePendingSharedVideoRouteIfNeeded()
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
            KeepScreenOnEffect(true)
            if (state.isCardLayoutEnabled) {
                YRALReelPlayerCardStack(
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
                        // For card stack, any edge swipe attempt should trigger load more
                        onEdgeScrollAttempt(page)
                    },
                ) { pageNo, scrollToNext ->
                    FeedOverlay(
                        pageNo = pageNo,
                        state = state,
                        topOverlay = { topOverlay(pageNo) },
                        bottomOverlay = { bottomOverlay(pageNo, scrollToNext) },
                        actionsRight = { actionsRight(pageNo) },
                        requestLoginFactory = component.requestLoginFactory,
                    )
                }
            } else {
                YRALReelPlayer(
                    modifier = Modifier.weight(1f),
                    reels = getReels(state),
                    maxReelsInPager = limitReelCount,
                    initialPage = state.currentPageOfFeed,
                    onPageLoaded = { page ->
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
                ) { pageNo, scrollToNext ->
                    FeedOverlay(
                        pageNo = pageNo,
                        state = state,
                        topOverlay = { topOverlay(pageNo) },
                        bottomOverlay = { bottomOverlay(pageNo, scrollToNext) },
                        actionsRight = { actionsRight(pageNo) },
                        requestLoginFactory = component.requestLoginFactory,
                    )
                }
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

    LaunchedEffect(Unit) {
        viewModel.feedEvents.collect { event ->
            when (event) {
                is FeedEvents.FollowedSuccessfully -> {
                    ToastManager.showSuccess(
                        type =
                            ToastType.Small(
                                message =
                                    getString(
                                        DesignRes.string.started_following,
                                        event.userName,
                                    ),
                            ),
                    )
                    if (state.currentPageOfFeed % FOLLOW_NUDGE_PAGE == 0) {
                        component.showAlertsOnDialog(AlertsRequestType.FOLLOW_BACK)
                    }
                }
                is FeedEvents.UnfollowedSuccessfully -> Unit
                is FeedEvents.Failed -> ToastManager.showError(type = ToastType.Small(message = event.message))
            }
        }
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
    topOverlay: @Composable () -> Unit,
    bottomOverlay: @Composable () -> Unit,
    actionsRight: @Composable ColumnScope.() -> Unit,
    requestLoginFactory: RequestLoginFactory,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        if (state.currentOnboardingStep == OnboardingStep.INTRO_RANK ||
            state.currentOnboardingStep == OnboardingStep.INTRO_BALANCE
        ) {
            BottomView(
                bottomOverlay = bottomOverlay,
                actionsRight = actionsRight,
            )
            topOverlay()
        } else {
            topOverlay()
            BottomView(
                bottomOverlay = bottomOverlay,
                actionsRight = actionsRight,
            )
        }
    }

    val loginState = rememberLoginInfo(requestLoginFactory = requestLoginFactory)
    LaunchedEffect(pageNo, state.isLoggedIn) {
        // Login overlay - rendered after Box to ensure it's on top
        if (!state.isLoggedIn && pageNo != 0 && (pageNo % SIGN_UP_PAGE) == 0) {
            loginState.requestLogin(
                SignupPageName.HOME,
                LoginScreenType.Overlay,
                LoginMode.BOTH,
                null,
                null,
            ) { LoginBottomContent() }
        } else {
            loginState.clearLogin()
        }
    }
    loginState.loginInfo?.let { loginInfo ->
        if (loginInfo.screenType is LoginScreenType.Overlay) {
            requestLoginFactory(loginInfo)()
        }
    }
}

@Composable
private fun BottomView(
    actionsRight: @Composable ColumnScope.() -> Unit,
    bottomOverlay: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Shadow(Modifier.align(Alignment.BottomCenter))
        ActionsRight(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 105.dp),
            actionsRight = actionsRight,
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
    actionsRight: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        actionsRight()
    }
}

@Composable
private fun LoginBottomContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.scroll_to_next_video),
            style = LocalAppTopography.current.mdBold,
            color = YralColors.NeutralIconsActive,
        )
        YralLottieAnimation(
            modifier = Modifier.size(36.dp),
            LottieRes.SIGNUP_SCROLL,
        )
    }
}

@Composable
internal expect fun KeepScreenOnEffect(keepScreenOn: Boolean)
