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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.model.PlayerConfig
import com.yral.shared.libs.videoPlayer.model.PlayerControls
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.pool.rememberPlayerPool
import com.yral.shared.libs.videoPlayer.util.PrefetchVideo
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListenerCreator
import com.yral.shared.libs.videoPlayer.util.rememberPrefetchPlayerWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun YRALReelPlayer(
    modifier: Modifier = Modifier,
    videoUrlArray: List<ReelPlayerItem>,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    didVideoEnd: () -> Unit,
    getPlayerListener: (creator: PrefetchVideoListenerCreator) -> PrefetchVideoListener,
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
        getPlayerListener = getPlayerListener,
        overlayContent = overlayContent,
    )
}

@Suppress("LongMethod")
@Composable
internal fun YRALReelsPlayerView(
    modifier: Modifier = Modifier, // Modifier for the composable
    urls: List<ReelPlayerItem>, // List of video URLs
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    playerConfig: PlayerConfig = PlayerConfig(), // Configuration for the player,
    getPlayerListener: (creator: PrefetchVideoListenerCreator) -> PrefetchVideoListener,
    overlayContent: @Composable (pageNo: Int) -> Unit,
) {
    // Remember the state of the pager
    val pagerState =
        rememberPagerState(
            pageCount = {
                urls.size // Set the page count based on the number of URLs
            },
            initialPage = initialPage,
        )

    // Create multiplatform player pool for efficient resource management
    val playerPool = rememberPlayerPool(maxPoolSize = 3)
    // Clean up player pool when composable is disposed
    DisposableEffect(playerPool) {
        onDispose {
            playerPool.dispose()
        }
    }
    // Prefetch state management
    val prefetchQueue = remember { mutableStateSetOf<Pair<String, String>>() }
    val prefetchedUrls = remember { mutableStateSetOf<String>() }
    // Add new videos to prefetch queue on page change
    LaunchedEffect(urls, pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { currentPage ->
                val newUrls =
                    urls
                        .nextN(currentPage, PREFETCH_NEXT_N_VIDEOS)
                        .map { it.videoUrl to it.videoId }
                        .filter { prefetch -> prefetch.first !in prefetchedUrls }
                if (newUrls.isNotEmpty()) {
                    prefetchQueue.addAll(newUrls)
                }
            }
    }
    val prefetch by remember { derivedStateOf { prefetchQueue.firstOrNull() } }
    val prefetchVideoListener =
        remember(prefetch) {
            prefetch?.let { item ->
                getPlayerListener(
                    PrefetchVideoListenerCreator(
                        videoId = item.second,
                        url = item.first,
                        onUrlReady = {
                            prefetchQueue.removeIf { it.first == item.first }
                            prefetchedUrls.add(item.first)
                        },
                    ),
                )
            }
        }
    prefetch?.let { item ->
        PrefetchVideos(
            url = item.first,
            listener = prefetchVideoListener,
        )
    }

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
                    playerData = urls[page].toPlayerData(urls, page),
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
                    playerData = urls[page].toPlayerData(urls, page),
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
) {
    val prefetchPlayer = rememberPrefetchPlayerWithLifecycle()
    url?.let {
        PrefetchVideo(
            player = prefetchPlayer,
            url = url,
            listener = listener,
        )
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

data class ReelPlayerItem(
    val videoUrl: String,
    val thumbnailUrl: String,
    val videoId: String,
)

fun ReelPlayerItem.toPlayerData(
    urls: List<ReelPlayerItem>,
    page: Int,
): PlayerData =
    PlayerData(
        videoId = videoId,
        url = videoUrl,
        thumbnailUrl = thumbnailUrl,
        prefetchThumbnails =
            urls
                .nextN(page, PREFETCH_NEXT_N_THUMBNAILS)
                .map { it.thumbnailUrl },
    )

private const val PREFETCH_NEXT_N_THUMBNAILS = 3
private const val PREFETCH_NEXT_N_VIDEOS = 4
