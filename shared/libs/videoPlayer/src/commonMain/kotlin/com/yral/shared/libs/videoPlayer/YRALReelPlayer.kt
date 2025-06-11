package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.model.PlayerConfig
import com.yral.shared.libs.videoPlayer.model.PlayerControls
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.pool.rememberPlayerPool
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun YRALReelPlayer(
    modifier: Modifier = Modifier,
    videoUrlArray: List<Pair<String, String>>,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    didVideoEnd: () -> Unit,
    overlayContent: @Composable (pageNo: Int) -> Unit,
) {
    YRALReelsPlayerView(
        modifier = modifier.fillMaxSize(),
        urls = videoUrlArray,
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
        overlayContent = overlayContent,
    )
}

@Suppress("LongMethod")
@Composable
internal fun YRALReelsPlayerView(
    modifier: Modifier = Modifier, // Modifier for the composable
    urls: List<Pair<String, String>>, // List of video URLs
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    playerConfig: PlayerConfig = PlayerConfig(), // Configuration for the player,
    overlayContent: @Composable (pageNo: Int) -> Unit,
) {
    // Create multiplatform player pool for efficient resource management
    val playerPool = rememberPlayerPool(maxPoolSize = 3)

    // Clean up player pool when composable is disposed
    DisposableEffect(playerPool) {
        onDispose {
            playerPool.dispose()
        }
    }

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

    var isPause by remember { mutableStateOf(false) } // State for pausing/resuming video

    // Render vertical pager if enabled, otherwise render horizontal pager
    if (playerConfig.reelVerticalScrolling) {
        VerticalPager(
            modifier = modifier,
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
                    playerData =
                        PlayerData(
                            url = urls[page].first,
                            thumbnailUrl = urls[page].second,
                            prefetchThumbnails =
                                urls
                                    .nextN(page, PREFETCH_NEXT_N_THUMBNAILS)
                                    .map { it.second },
                            prefetchVideos =
                                urls
                                    .nextN(page, PREFETCH_NEXT_N_VIDEOS)
                                    .map { it.first },
                        ),
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
                    isPlayerVisible = pagerState.currentPage == page,
                )
                overlayContent(page)
            }
        }
    } else {
        HorizontalPager(
            modifier = modifier,
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
                    playerData =
                        PlayerData(
                            url = urls[page].first,
                            thumbnailUrl = urls[page].second,
                            prefetchThumbnails =
                                urls
                                    .nextN(page, PREFETCH_NEXT_N_THUMBNAILS)
                                    .map { it.second },
                            prefetchVideos =
                                urls
                                    .nextN(page, PREFETCH_NEXT_N_VIDEOS)
                                    .map { it.first },
                        ),
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
                    isPlayerVisible = pagerState.currentPage == page,
                )
                overlayContent(page)
            }
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
