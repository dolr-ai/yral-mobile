package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.model.PlayerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun YRALReelPlayer(
    videoUrlArray: List<Pair<String, String>>,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
) {
    YRALReelsPlayerView(
        modifier = Modifier.fillMaxSize(),
        urls = videoUrlArray,
        initialPage = initialPage,
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
            ),
        onPageLoaded = onPageLoaded,
    )
}

@Suppress("LongMethod")
@Composable
internal fun YRALReelsPlayerView(
    modifier: Modifier = Modifier, // Modifier for the composable
    urls: List<Pair<String, String>>, // List of video URLs
    initialPage: Int,
    playerConfig: PlayerConfig = PlayerConfig(), // Configuration for the player,
    onPageLoaded: (currentPage: Int) -> Unit,
) {
    // Remember the state of the pager
    val pagerState =
        rememberPagerState(
            pageCount = {
                urls.size // Set the page count based on the number of URLs
            },
            initialPage = initialPage,
        )

    // Report initial pager state
    LaunchedEffect(Unit) {
        // Call the callback with the initial page to make sure it's registered
        onPageLoaded(pagerState.currentPage)
    }

    // Animate scrolling to the current page when it changes
    LaunchedEffect(key1 = pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            pagerState.animateScrollToPage(page)
        }
    }

    var showControls by remember { mutableStateOf(true) } // State for showing/hiding controls
    var isSeekbarSliding = false // Flag for indicating if the seek bar is being slid
    var isFullScreen by remember { mutableStateOf(false) }

    // Auto-hide controls if enabled
    if (playerConfig.isAutoHideControlEnabled) {
        LaunchedEffect(showControls) {
            if (showControls) {
                delay(timeMillis = (playerConfig.controlHideIntervalSeconds * 1000).toLong()) // Delay hiding controls
                if (isSeekbarSliding.not()) {
                    showControls = false // Hide controls if seek bar is not being slid
                }
            }
        }
    }

    // Render vertical pager if enabled, otherwise render horizontal pager
    if (playerConfig.reelVerticalScrolling) {
        VerticalPager(
            modifier = modifier,
            state = pagerState,
            userScrollEnabled = true, // Ensure user scrolling is enabled
        ) { page ->
            // Create a side effect to detect when this page is shown
            LaunchedEffect(page, pagerState.currentPage) {
                if (pagerState.currentPage == page) {
                    // Call the callback directly from here
                    onPageLoaded(page)
                }
            }
            var isPause by remember { mutableStateOf(false) } // State for pausing/resuming video
            // Video player with control
            YRALVideoPlayerWithControl(
                modifier = Modifier.fillMaxSize(),
                url = urls[page].first,
                thumbnailUrl = urls[page].second,
                prefetchThumbnails = urls.nextN(page, PREFETCH_NEXT_N_THUMBNAILS).map { it.second },
                prefetchVideos = urls.nextN(page, PREFETCH_NEXT_N_VIDEOS).map { it.first },
                playerConfig = playerConfig,
                isPause =
                    if (pagerState.currentPage == page) {
                        isPause
                    } else {
                        true
                    }, // Pause video when not in focus
                onPauseToggle = { isPause = isPause.not() }, // Toggle pause/resume
                showControls = showControls, // Show/hide controls
                onShowControlsToggle = {
                    showControls = showControls.not()
                }, // Toggle show/hide controls
                onChangeSeekbar = { isSeekbarSliding = it }, // Update seek bar sliding state
                isFullScreen = isFullScreen,
                onFullScreenToggle = { isFullScreen = isFullScreen.not() },
            )
        }
    } else {
        HorizontalPager(
            modifier = modifier,
            state = pagerState,
            userScrollEnabled = true, // Ensure user scrolling is enabled
        ) { page ->
            // Create a side effect to detect when this page is shown
            LaunchedEffect(page, pagerState.currentPage) {
                if (pagerState.currentPage == page) {
                    // Call the callback directly from here
                    onPageLoaded(page)
                }
            }
            var isPause by remember { mutableStateOf(false) } // State for pausing/resuming video
            // Video player with control
            YRALVideoPlayerWithControl(
                modifier = Modifier.fillMaxSize(),
                url = urls[page].first, // URL of the video
                thumbnailUrl = urls[page].second,
                playerConfig = playerConfig,
                isPause =
                    if (pagerState.currentPage == page) {
                        isPause
                    } else {
                        true
                    }, // Pause video when not in focus
                onPauseToggle = { isPause = isPause.not() }, // Toggle pause/resume
                showControls = showControls, // Show/hide controls
                onShowControlsToggle = {
                    showControls = showControls.not()
                }, // Toggle show/hide controls
                onChangeSeekbar = { isSeekbarSliding = it }, // Update seek bar sliding state
                isFullScreen = isFullScreen,
                onFullScreenToggle = { isFullScreen = isFullScreen.not() },
            )
        }
    }
}

private fun <T> List<T>.nextN(
    startIndex: Int,
    n: Int,
): List<T> =
    if (startIndex + 1 < size) {
        subList(startIndex + 1, minOf(startIndex + n, size))
    } else {
        emptyList()
    }

private const val PREFETCH_NEXT_N_THUMBNAILS = 3
private const val PREFETCH_NEXT_N_VIDEOS = 3
