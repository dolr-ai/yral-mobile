package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.yral.shared.libs.videoPlayer.model.PREFETCH_NEXT_N_VIDEOS
import com.yral.shared.libs.videoPlayer.model.PlayerConfig
import com.yral.shared.libs.videoPlayer.model.PlayerControls
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.model.toPlayerData
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.pool.rememberPlayerPool
import com.yral.shared.libs.videoPlayer.util.EdgeScrollDetectConnection
import com.yral.shared.libs.videoPlayer.util.PrefetchVideo
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import com.yral.shared.libs.videoPlayer.util.ReelScrollDirection
import com.yral.shared.libs.videoPlayer.util.nextN
import com.yral.shared.libs.videoPlayer.util.rememberPrefetchPlayerWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun YRALReelPlayer(
    modifier: Modifier = Modifier,
    reels: List<Reels>,
    maxReelsInPager: Int,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    didVideoEnd: () -> Unit,
    onEdgeScrollAttempt: (pageNo: Int, atStart: Boolean, direction: ReelScrollDirection) -> Unit = { _, _, _ -> },
    getPrefetchListener: (reel: Reels) -> PrefetchVideoListener,
    getVideoListener: (reel: Reels) -> VideoListener?,
    overlayContent: @Composable (pageNo: Int) -> Unit,
) {
    YRALReelsPlayerView(
        modifier = modifier.fillMaxSize(),
        reels = reels,
        maxReelsInPager = maxReelsInPager,
        initialPage = initialPage,
        onPageLoaded = onPageLoaded,
        recordTime = recordTime,
        playerConfig =
            PlayerConfig(
                isAutoHideControlEnabled = true,
                isPauseResumeEnabled = false,
                isFastForwardBackwardEnabled = false,
                isSeekBarVisible = false,
                isDurationVisible = false,
                isMuteControlEnabled = false,
                isSpeedControlEnabled = false,
                isFullScreenEnabled = false,
                isScreenLockEnabled = false,
                reelVerticalScrolling = true,
                loaderView = {},
                didEndVideo = didVideoEnd,
            ),
        onEdgeScrollAttempt = onEdgeScrollAttempt,
        getPrefetchListener = getPrefetchListener,
        getVideoListener = { getVideoListener(it) },
        overlayContent = overlayContent,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun YRALReelsPlayerView(
    modifier: Modifier = Modifier, // Modifier for the composable
    reels: List<Reels>, // List of video URLs
    maxReelsInPager: Int,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    playerConfig: PlayerConfig = PlayerConfig(), // Configuration for the player,
    onEdgeScrollAttempt: (pageNo: Int, atStart: Boolean, direction: ReelScrollDirection) -> Unit = { _, _, _ -> },
    getPrefetchListener: (reel: Reels) -> PrefetchVideoListener,
    getVideoListener: (reel: Reels) -> VideoListener?,
    overlayContent: @Composable (pageNo: Int) -> Unit,
) {
    val pageCount by remember(reels, maxReelsInPager) {
        derivedStateOf { minOf(reels.size, maxReelsInPager) }
    }
    // Remember the state of the pager
    val pagerState =
        rememberPagerState(
            pageCount = { pageCount },
            initialPage = initialPage.coerceAtMost(pageCount - 1),
        )

    // Create multiplatform player pool for efficient resource management
    val playerPool = rememberPlayerPool(maxPoolSize = 3)
    // Clean up player pool when composable is disposed
    DisposableEffect(playerPool) { onDispose { playerPool.dispose() } }

    // Prefetch state management
    val prefetchQueue = remember { mutableStateSetOf<Reels>() }
    val prefetchedReels = remember { mutableStateSetOf<String>() }
    // Add new videos to prefetch queue on page change
    LaunchedEffect(reels, pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { currentPage ->
                val newReels =
                    reels
                        .nextN(currentPage, PREFETCH_NEXT_N_VIDEOS)
                        .filter { prefetch -> prefetch.videoId !in prefetchedReels }
                if (newReels.isNotEmpty()) {
                    prefetchQueue.addAll(newReels)
                }
            }
    }
    val prefetch by remember { derivedStateOf { prefetchQueue.firstOrNull() } }
    val prefetchVideoListener = remember(prefetch) { prefetch?.let { reel -> getPrefetchListener(reel) } }
    prefetch?.let { reel ->
        PrefetchVideos(
            url = reel.videoUrl,
            listener = prefetchVideoListener,
            onUrlReady = {
                prefetchQueue.removeAll { it.videoId == reel.videoId }
                prefetchedReels.add(reel.videoId)
            },
        )
    }

    // Report initial pager state
    LaunchedEffect(Unit) { onPageLoaded(pagerState.currentPage) }

    // Animate scrolling to the current page when it changes
    LaunchedEffect(key1 = pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> pagerState.animateScrollToPage(page) }
    }

    // Page change to start/stop play back time trace
    var lastPage by remember { mutableIntStateOf(-1) }
    LaunchedEffect(key1 = pagerState, reels) {
        snapshotFlow { pagerState.currentPage to reels }
            .distinctUntilChanged()
            .collect { (page, reels) ->
                if (lastPage != page) {
                    if (reels.size > lastPage && lastPage >= 0) {
                        playerPool.onPlayBackStopped(
                            playerData = reels[lastPage].toPlayerData(),
                        )
                    }
                    // small delay for player setup
                    delay(PLAYER_SETUP_DELAY)
                    if (reels.size > page && page >= 0) {
                        playerPool.onPlayBackStarted(
                            playerData = reels[page].toPlayerData(),
                        )
                    }
                    lastPage = page
                }
            }
    }

    var isPause by remember { mutableStateOf(false) } // State for pausing/resuming video

    // Detect user attempts to scroll beyond available pages (start or end)
    val edgeDetectConnection =
        remember(pageCount, pagerState.currentPage, playerConfig.reelVerticalScrolling) {
            EdgeScrollDetectConnection(
                pageCount = pageCount,
                pagerState = pagerState,
                playerConfig = playerConfig,
                onEdgeScrollAttempt = onEdgeScrollAttempt,
            )
        }

    // Render vertical pager if enabled, otherwise render horizontal pager
    if (playerConfig.reelVerticalScrolling) {
        VerticalPager(
            modifier = modifier.nestedScroll(edgeDetectConnection),
            state = pagerState,
            userScrollEnabled = true, // Ensure user scrolling is enabled
            beyondViewportPageCount = 0,
            key = { page -> reels.getOrNull(page)?.videoId ?: page },
        ) { page ->
            // Create a side effect to detect when this page is shown
            LaunchedEffect(page, pagerState.currentPage) {
                if (pagerState.currentPage == page) {
                    // Call the callback directly from here
                    onPageLoaded(page)
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
                // Video player with control
                YRALVideoPlayerWithControl(
                    modifier = Modifier.fillMaxSize(),
                    playerData = reels[page].toPlayerData(reels, page),
                    playerConfig = playerConfig,
                    playerControls =
                        PlayerControls(
                            isPause =
                                if (pagerState.currentPage == page) {
                                    isPause
                                } else {
                                    true
                                }, // Pause video when not in focus
                            onPauseToggle = { isPause = isPause.not() }, // Toggle pause/resume
                            recordTime = recordTime,
                        ),
                    playerPool = playerPool,
                    videoListener = getVideoListener(reels[page]),
                )
                overlayContent(page)
            }
        }
    } else {
        HorizontalPager(
            modifier = modifier.nestedScroll(edgeDetectConnection),
            state = pagerState,
            userScrollEnabled = true, // Ensure user scrolling is enabled
            beyondViewportPageCount = 0,
        ) { page ->
            // Create a side effect to detect when this page is shown
            LaunchedEffect(page, pagerState.currentPage) {
                if (pagerState.currentPage == page) {
                    // Call the callback directly from here
                    onPageLoaded(page)
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
                // Video player with control
                YRALVideoPlayerWithControl(
                    modifier = Modifier.fillMaxSize(),
                    playerData = reels[page].toPlayerData(reels, page),
                    playerConfig = playerConfig,
                    playerControls =
                        PlayerControls(
                            isPause =
                                if (pagerState.currentPage == page) {
                                    isPause
                                } else {
                                    true
                                }, // Pause video when not in focus
                            onPauseToggle = { isPause = isPause.not() }, // Toggle pause/resume
                            recordTime = recordTime,
                        ),
                    playerPool = playerPool,
                    videoListener = getVideoListener(reels[page]),
                )
                overlayContent(page)
            }
        }
    }
}

@Composable
private fun PrefetchVideos(
    url: String?,
    listener: PrefetchVideoListener?,
    onUrlReady: (url: String) -> Unit,
) {
    val prefetchPlayer = rememberPrefetchPlayerWithLifecycle()
    url?.let {
        PrefetchVideo(
            player = prefetchPlayer,
            url = url,
            listener = listener,
            onUrlReady = onUrlReady,
        )
    }
}

private const val PLAYER_SETUP_DELAY = 100L
