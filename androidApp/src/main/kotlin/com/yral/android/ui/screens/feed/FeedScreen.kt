package com.yral.android.ui.screens.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.feed.FeedScreenConstants.MAX_LINES_FOR_POST_DESCRIPTION
import com.yral.android.ui.screens.feed.FeedScreenConstants.VIDEO_REPORT_SHEET_MAX_HEIGHT
import com.yral.android.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.android.ui.screens.feed.performance.VideoListenerImpl
import com.yral.android.ui.screens.game.AboutGameSheet
import com.yral.android.ui.screens.game.CoinBalance
import com.yral.android.ui.screens.game.GameResultSheet
import com.yral.android.ui.screens.game.SmileyGame
import com.yral.android.ui.widgets.PreloadLottieAnimation
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.YralButtonState
import com.yral.android.ui.widgets.YralButtonType
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST
import com.yral.shared.features.feed.viewmodel.ReportSheetState
import com.yral.shared.features.feed.viewmodel.VideoReportReason
import com.yral.shared.features.game.viewmodel.GameState
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.rust.domain.models.FeedDetails
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
    gameViewModel: GameViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val gameState by gameViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    // Keep track of the last feed size to detect when new items are loaded
    var lastFeedSize by remember { mutableIntStateOf(0) }
    // Track when a load was triggered to prevent multiple calls
    var loadTriggered by remember { mutableStateOf(false) }
    // Track if new items have been loaded since loading started
    var newItemsAddedSinceLoading by remember { mutableStateOf(false) }

    // Set initial video ID when feed loads
    LaunchedEffect(state.feedDetails.isNotEmpty()) {
        if (state.currentPageOfFeed < state.feedDetails.size) {
            gameViewModel.setCurrentVideoId(state.feedDetails[state.currentPageOfFeed].videoID)
        }
    }

    // Function to determine if we should load more content
    val shouldLoadMore =
        remember(
            state.currentPageOfFeed,
            state.feedDetails.size,
            state.isLoadingMore,
            loadTriggered,
        ) {
            val isValidPage = state.currentPageOfFeed >= 0
            val isCloseToEnd =
                state.feedDetails.isNotEmpty() &&
                    (state.feedDetails.size - state.currentPageOfFeed) <= PRE_FETCH_BEFORE_LAST
            isValidPage && isCloseToEnd && !state.isLoadingMore && !loadTriggered
        }

    // Reset load triggered when feed size changes (meaning new data arrived)
    LaunchedEffect(state.feedDetails.size) {
        if (state.feedDetails.size > lastFeedSize) {
            // New items have been added
            loadTriggered = false
            lastFeedSize = state.feedDetails.size
            // Mark that we've received new items since loading started
            if (state.isLoadingMore) {
                newItemsAddedSinceLoading = true
            }
        }
    }

    // Reset states when loading state changes
    LaunchedEffect(state.isLoadingMore) {
        if (state.isLoadingMore) {
            // Reset flag when loading starts
            newItemsAddedSinceLoading = false
        } else {
            // Reset load triggered when loading completes
            loadTriggered = false
        }
    }

    // Handle loading more when currentPage changes
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadTriggered = true
            coroutineScope.launch {
                viewModel.loadMoreFeed()
            }
        }
    }

    // Determine if we should show the loader
    val showLoader = state.isLoadingMore && !newItemsAddedSinceLoading

    Column(modifier = modifier) {
        if (state.feedDetails.isNotEmpty()) {
            YRALReelPlayer(
                modifier = Modifier.weight(1f),
                reels = getReels(state),
                initialPage = state.currentPageOfFeed,
                onPageLoaded = { page ->
                    // Mark animation as shown for the previous page when changing pages
                    if (viewModel.shouldMarkAnimationAsCompleted(page)) {
                        gameViewModel.markCoinDeltaAnimationShown(
                            videoId = state.feedDetails[state.currentPageOfFeed].videoID,
                        )
                    }
                    // Set current video ID for the new page
                    if (page < state.feedDetails.size) {
                        gameViewModel.setCurrentVideoId(state.feedDetails[page].videoID)
                    }
                    viewModel.onCurrentPageChange(page)
                    viewModel.setPostDescriptionExpanded(false)
                },
                recordTime = { currentTime, totalTime ->
                    viewModel.recordTime(currentTime, totalTime)
                },
                didVideoEnd = { viewModel.didCurrentVideoEnd() },
                getPrefetchListener = { PrefetchVideoListenerImpl(it) },
                getVideoListener = { reel ->
                    VideoListenerImpl(
                        reel = reel,
                        registerTrace = { id, tractType ->
                            viewModel.registerTrace(id, tractType.name)
                        },
                        isTraced = { id, traceType ->
                            viewModel.isAlreadyTraced(id, traceType.name)
                        },
                    )
                },
            ) { pageNo ->
                FeedOverlay(
                    pageNo = pageNo,
                    state = state,
                    feedViewModel = viewModel,
                    gameState = gameState,
                    gameViewModel = gameViewModel,
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
                    YralLoader(size = 32.dp)
                }
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
            isLoading = state.isLoading,
            reasons = (state.reportSheetState as ReportSheetState.Open).reasons,
            onSubmit = { reason, text ->
                viewModel.reportVideo(
                    reason = reason,
                    text = text,
                    pageNo = (state.reportSheetState as ReportSheetState.Open).pageNo,
                )
            },
        )
    }
    if (gameState.showResultSheet && state.currentPageOfFeed < state.feedDetails.size) {
        val currentVideoId = state.feedDetails[state.currentPageOfFeed].videoID
        GameResultSheet(
            coinDelta = gameViewModel.getFeedGameResult(currentVideoId),
            gameIcon = gameState.gameResult[currentVideoId]?.first,
            onDismissRequest = {
                gameViewModel.toggleResultSheet(false)
            },
            openAboutGame = {
                gameViewModel.toggleAboutGame(true)
            },
        )
    }
    if (gameState.showAboutGame && state.currentPageOfFeed < state.feedDetails.size) {
        AboutGameSheet(
            gameRules = gameState.gameRules,
            onDismissRequest = {
                gameViewModel.toggleAboutGame(false)
            },
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
        videoUrl = url.toString(),
        thumbnailUrl = thumbnail.toString(),
        videoId = videoID,
    )

@Composable
private fun FeedOverlay(
    pageNo: Int,
    state: FeedState,
    feedViewModel: FeedViewModel,
    gameState: GameState,
    gameViewModel: GameViewModel,
) {
    var lottieCached by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        TopView(
            state = state,
            pageNo = pageNo,
            feedViewModel = feedViewModel,
            gameState = gameState,
            gameViewModel = gameViewModel,
        )
        ReportVideo(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 89.dp),
        ) {
            feedViewModel.toggleReportSheet(true, pageNo)
        }
        if (gameState.gameIcons.isNotEmpty()) {
            SmileyGame(
                gameIcons = gameState.gameIcons,
                clickedIcon = gameState.gameResult[state.feedDetails[pageNo].videoID]?.first,
                onIconClicked = {
                    gameViewModel.setClickedIcon(
                        icon = it,
                        videoId = state.feedDetails[pageNo].videoID,
                    )
                },
                coinDelta = gameViewModel.getFeedGameResult(state.feedDetails[pageNo].videoID),
                errorMessage = gameViewModel.getFeedGameResultError(state.feedDetails[pageNo].videoID),
                isLoading = gameState.isLoading,
                hasShownCoinDeltaAnimation =
                    gameViewModel.hasShownCoinDeltaAnimation(
                        state.feedDetails[pageNo].videoID,
                    ),
                onDeltaAnimationComplete = {
                    gameViewModel.markCoinDeltaAnimationShown(
                        state.feedDetails[pageNo].videoID,
                    )
                },
            )
            if (!lottieCached) {
                gameState.gameIcons.forEach { icon ->
                    PreloadLottieAnimation(icon.clickAnimation)
                }
                lottieCached = true
            }
        }
    }
}

@Composable
private fun TopView(
    state: FeedState,
    pageNo: Int,
    feedViewModel: FeedViewModel,
    gameState: GameState,
    gameViewModel: GameViewModel,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        var paddingEnd by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(density) { (configuration.screenWidthDp.dp).toPx() }
        UserBrief(
            principalId = state.feedDetails[pageNo].principalID,
            profileImageUrl = state.feedDetails[pageNo].profileImageURL,
            postDescription = state.feedDetails[pageNo].postDescription,
            isPostDescriptionExpanded = state.isPostDescriptionExpanded,
            setPostDescriptionExpanded = { feedViewModel.setPostDescriptionExpanded(it) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = with(density) { paddingEnd.toDp() + 46.dp }),
        )
        CoinBalance(
            coinBalance = gameState.coinBalance,
            coinDelta = gameState.lastBalanceDifference,
            animateBag = gameState.animateCoinBalance,
            setAnimate = { gameViewModel.setAnimateCoinBalance(it) },
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .onGloballyPositioned { coordinates ->
                        if (!gameState.animateCoinBalance) {
                            val x = coordinates.positionInParent().x
                            paddingEnd = screenWidthPx - x
                        }
                    },
        )
    }
}

@Composable
private fun UserBrief(
    profileImageUrl: Url?,
    principalId: String,
    postDescription: String,
    isPostDescriptionExpanded: Boolean,
    setPostDescriptionExpanded: (isExpanded: Boolean) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(
                    top = 22.dp,
                    start = 16.dp,
                    bottom = 22.dp,
                ),
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.user_brief),
            contentDescription = "image description",
            contentScale = ContentScale.FillBounds,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                        bottom = 8.dp,
                        start = 8.dp,
                    ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserBriefProfileImage(profileImageUrl)
            UserBriefDetails(
                modifier = Modifier.weight(1f),
                principalId = principalId,
                postDescription = postDescription,
                isPostDescriptionExpanded = isPostDescriptionExpanded,
                setPostDescriptionExpanded = setPostDescriptionExpanded,
            )
        }
    }
}

@Composable
private fun UserBriefProfileImage(profileImageUrl: Url?) {
    val shape = RoundedCornerShape(size = 40.dp)
    AsyncImage(
        model = profileImageUrl.toString(),
        contentDescription = "User picture",
        contentScale = ContentScale.FillBounds,
        modifier =
            Modifier
                .clip(shape)
                .border(
                    width = 2.dp,
                    color = YralColors.Pink300,
                    shape = shape,
                ).width(40.dp)
                .height(40.dp)
                .background(
                    color = YralColors.ProfilePicBackground,
                    shape = shape,
                ),
    )
}

@Composable
private fun UserBriefDetails(
    modifier: Modifier,
    principalId: String,
    postDescription: String,
    isPostDescriptionExpanded: Boolean,
    setPostDescriptionExpanded: (isExpanded: Boolean) -> Unit,
) {
    Column(
        modifier =
            modifier
                .clickable {
                    setPostDescriptionExpanded(!isPostDescriptionExpanded)
                },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = principalId,
            style = LocalAppTopography.current.feedCanisterId,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isPostDescriptionExpanded) {
            val scrollState = rememberScrollState()
            val maxHeight =
                LocalAppTopography
                    .current
                    .feedDescription
                    .lineHeight
                    .value * MAX_LINES_FOR_POST_DESCRIPTION
            Text(
                modifier =
                    Modifier
                        .heightIn(max = maxHeight.dp)
                        .verticalScroll(scrollState),
                text = postDescription,
                style = LocalAppTopography.current.feedDescription,
                color = Color.White,
            )
        } else {
            Text(
                text = postDescription,
                style = LocalAppTopography.current.feedDescription,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReportVideo(
    modifier: Modifier = Modifier,
    onReportClicked: () -> Unit,
) {
    Box(modifier = modifier) {
        Image(
            modifier =
                Modifier
                    .size(36.dp)
                    .padding(1.dp)
                    .clickable { onReportClicked() },
            painter = painterResource(id = R.drawable.exclamation),
            contentDescription = "image description",
            contentScale = ContentScale.None,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportVideoSheet(
    onDismissRequest: () -> Unit,
    bottomSheetState: SheetState,
    isLoading: Boolean,
    reasons: List<VideoReportReason>,
    onSubmit: (reason: VideoReportReason, text: String) -> Unit,
) {
    var selectedReason by remember { mutableStateOf<VideoReportReason?>(null) }
    var text by remember { mutableStateOf("") }
    val buttonState =
        when {
            isLoading -> YralButtonState.Loading
            selectedReason == null -> YralButtonState.Disabled
            selectedReason == VideoReportReason.OTHERS && text.isEmpty() -> YralButtonState.Disabled
            else -> YralButtonState.Enabled
        }
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight(VIDEO_REPORT_SHEET_MAX_HEIGHT)
                    .padding(
                        start = 16.dp,
                        top = 28.dp,
                        end = 16.dp,
                        bottom = 36.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VideoReportSheetTitle()
            VideoReportReasons(
                modifier = Modifier.weight(1f),
                reasons = reasons,
                selectedReason = selectedReason,
                onSelected = { selectedReason = it },
                text = text,
                onTextUpdate = { text = it },
            )
            YralGradientButton(
                text = stringResource(R.string.submit),
                buttonType = YralButtonType.White,
                buttonState = buttonState,
            ) {
                selectedReason?.let {
                    onSubmit(it, text)
                }
            }
        }
    }
}

@Composable
private fun VideoReportSheetTitle() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.report_video),
            style = LocalAppTopography.current.xlSemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.report_video_question),
            style = LocalAppTopography.current.regRegular,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VideoReportReasons(
    modifier: Modifier,
    reasons: List<VideoReportReason>,
    selectedReason: VideoReportReason?,
    onSelected: (reason: VideoReportReason) -> Unit,
    text: String,
    onTextUpdate: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedReason) {
        if (selectedReason == VideoReportReason.OTHERS) {
            listState.animateScrollToItem(reasons.size)
        }
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        items(reasons) { reason ->
            ReasonItem(
                reason = reason,
                isSelected = reason.name == selectedReason?.name,
                onClick = {
                    onSelected(reason)
                },
            )
        }
        if (selectedReason == VideoReportReason.OTHERS) {
            item {
                ReasonDetailsInput(
                    text = text,
                    onValueChange = onTextUpdate,
                )
            }
        }
    }
}

@Composable
private fun ReasonDetailsInput(
    text: String,
    onValueChange: (text: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.please_provide_more_details),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        TextField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
            value = text,
            onValueChange = onValueChange,
            colors =
                TextFieldDefaults.colors().copy(
                    focusedTextColor = YralColors.Neutral600,
                    unfocusedTextColor = YralColors.Neutral600,
                    disabledTextColor = YralColors.Neutral600,
                    focusedContainerColor = YralColors.Neutral800,
                    unfocusedContainerColor = YralColors.Neutral800,
                    disabledContainerColor = YralColors.Neutral800,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            textStyle = LocalAppTopography.current.baseRegular,
            placeholder = {
                Text(
                    text = stringResource(R.string.add_details),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.Neutral600,
                )
            },
        )
    }
}

@Composable
private fun ReasonItem(
    reason: VideoReportReason,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(color = YralColors.Neutral800, shape = RoundedCornerShape(size = 4.dp))
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier.size(18.dp),
            painter =
                painterResource(
                    id =
                        if (isSelected) {
                            R.drawable.radio_selected
                        } else {
                            R.drawable.radio_unselected
                        },
                ),
            contentDescription = "image description",
            contentScale = ContentScale.None,
        )
        Text(
            text = reason.displayText(),
            style = LocalAppTopography.current.baseMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun VideoReportReason.displayText(): String =
    when (this) {
        VideoReportReason.NUDITY_PORN -> stringResource(R.string.reason_nudity)
        VideoReportReason.VIOLENCE -> stringResource(R.string.reason_violence)
        VideoReportReason.OFFENSIVE -> stringResource(R.string.reason_offensive)
        VideoReportReason.SPAM -> stringResource(R.string.reason_spam)
        VideoReportReason.OTHERS -> stringResource(R.string.reason_others)
    }

object FeedScreenConstants {
    const val MAX_LINES_FOR_POST_DESCRIPTION = 5
    const val VIDEO_REPORT_SHEET_MAX_HEIGHT = 0.8f
}
