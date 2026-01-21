package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.videoPlayer.model.PREFETCH_NEXT_N_VIDEOS
import com.yral.shared.libs.videoPlayer.model.PlayerConfig
import com.yral.shared.libs.videoPlayer.model.PlayerControls
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.model.toPlayerData
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.pool.rememberPlayerPool
import com.yral.shared.libs.videoPlayer.util.PrefetchVideo
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import com.yral.shared.libs.videoPlayer.util.ReelScrollDirection
import com.yral.shared.libs.videoPlayer.util.evictPrefetchedVideo
import com.yral.shared.libs.videoPlayer.util.nextN
import com.yral.shared.libs.videoPlayer.util.rememberPrefetchPlayerWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * A Tinder-style swipeable card stack for video reels.
 *
 * Displays videos in a stacked card layout where users can swipe in any direction
 * (up, down, left, right) to dismiss the current video and reveal the next one.
 *
 * @param modifier Modifier for the card stack container.
 * @param reels List of video reels to display.
 * @param maxReelsInPager Maximum number of reels to allow (for limiting scroll).
 * @param initialPage Initial index to start at.
 * @param onPageLoaded Callback when a new page becomes visible.
 * @param recordTime Callback to record time spent on video (currentTime, totalTime).
 * @param playerConfig Configuration for the video player.
 * @param onEdgeScrollAttempt Callback when user tries to swipe past the end.
 * @param getPrefetchListener Factory for prefetch listeners.
 * @param getVideoListener Factory for video listeners.
 * @param overlayContent Content to overlay on each video card.
 * @param onSwipeVote Callback when a swipe vote is registered (direction, pageIndex before swipe).
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun SwipeableCardStack(
    modifier: Modifier = Modifier,
    reels: List<Reels>,
    maxReelsInPager: Int,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    playerConfig: PlayerConfig,
    onEdgeScrollAttempt: (pageNo: Int, atStart: Boolean, direction: ReelScrollDirection) -> Unit,
    getPrefetchListener: (reel: Reels) -> PrefetchVideoListener,
    getVideoListener: (reel: Reels) -> VideoListener?,
    overlayContent: @Composable (pageNo: Int, scrollToNext: () -> Unit) -> Unit,
    onSwipeVote: ((direction: SwipeDirection, pageIndex: Int) -> Unit)? = null,
) {
    val pageCount = minOf(reels.size, maxReelsInPager)
    if (pageCount == 0) return

    // Swipe state for the card stack
    val swipeState =
        rememberSwipeableCardState(
            initialIndex = initialPage.coerceAtMost(pageCount - 1),
            itemCount = pageCount,
        )

    // Update item count when reels change
    LaunchedEffect(pageCount) {
        swipeState.updateItemCount(pageCount)
    }

    // Create player pool for efficient resource management
    // Pool size 4 to support both front and next card video players simultaneously
    val playerPool = rememberPlayerPool(maxPoolSize = 4)
    DisposableEffect(playerPool) {
        onDispose { playerPool.dispose() }
    }

    // Prefetch state management (same as YRALReelPlayer)
    val prefetchQueue = remember { mutableStateSetOf<Reels>() }
    val prefetchedReels = remember { mutableStateSetOf<String>() }
    val prefetchedReelUrls = remember { mutableStateMapOf<String, String>() }

    DisposableEffect(Unit) {
        onDispose {
            prefetchedReelUrls.values.forEach { url ->
                evictPrefetchedVideo(url)
            }
            prefetchedReelUrls.clear()
            prefetchedReels.clear()
            prefetchQueue.clear()
        }
    }

    // Add new videos to prefetch queue on index change
    LaunchedEffect(reels, swipeState.currentIndex) {
        snapshotFlow { swipeState.currentIndex }
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

    // Process prefetch queue
    val prefetch by remember { derivedStateOf { prefetchQueue.firstOrNull() } }
    val prefetchVideoListener =
        remember(prefetch) {
            prefetch?.let { reel -> getPrefetchListener(reel) }
        }

    prefetch?.let { reel ->
        PrefetchVideos(
            url = reel.videoUrl,
            listener = prefetchVideoListener,
            onUrlReady = {
                prefetchQueue.removeAll { it.videoId == reel.videoId }
                prefetchedReels.add(reel.videoId)
                prefetchedReelUrls[reel.videoId] = reel.videoUrl
            },
        )
    }

    // Clean up prefetched videos outside window
    LaunchedEffect(reels, swipeState.currentIndex, prefetchedReelUrls) {
        snapshotFlow {
            val currentPage = swipeState.currentIndex
            val keepIds = mutableSetOf<String>()
            reels.getOrNull(currentPage)?.videoId?.let(keepIds::add)
            reels.getOrNull(currentPage - 1)?.videoId?.let(keepIds::add)
            reels
                .nextN(currentPage, PREFETCH_NEXT_N_VIDEOS)
                .forEach { keepIds.add(it.videoId) }
            keepIds.toSet() to prefetchedReelUrls.toMap()
        }.distinctUntilChanged().collect { (keepIds, prefetchedMap) ->
            prefetchedMap.forEach { (videoId, url) ->
                if (videoId !in keepIds) {
                    evictPrefetchedVideo(url)
                    prefetchedReels.remove(videoId)
                    prefetchedReelUrls.remove(videoId)
                }
            }
        }
    }

    // Report initial page
    LaunchedEffect(Unit) {
        onPageLoaded(swipeState.currentIndex)
    }

    // Page change to start/stop playback
    var lastPage by remember { mutableIntStateOf(-1) }
    LaunchedEffect(swipeState.currentIndex) {
        snapshotFlow { swipeState.currentIndex }
            .distinctUntilChanged()
            .collect { page ->
                if (lastPage != page) {
                    // Stop old video
                    if (reels.size > lastPage && lastPage >= 0) {
                        playerPool.onPlayBackStopped(
                            playerData = reels[lastPage].toPlayerData(),
                        )
                    }
                    // Start new video
                    val newVideo = reels.getOrNull(page)
                    if (newVideo != null) {
                        playerPool.onPlayBackStarted(
                            playerData = newVideo.toPlayerData(),
                        )
                    }
                    lastPage = page
                }
            }
    }

    // Pause state for current video
    var isPause by remember { mutableStateOf(false) }

    // Track playback positions by videoId (for resuming after transition)
    val videoPositions = remember { mutableStateMapOf<String, Int>() }

    // Reset pause state when page changes (ensures new video plays)
    LaunchedEffect(swipeState.currentIndex) {
        isPause = false
    }
    // Trigger scroll to next (for overlay button)
    var autoScrollToNext by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val coroutineScope = rememberCoroutineScope()

        // Calculate swipe progress for early playback determination
        val swipeProgress = swipeState.calculateSwipeProgress(screenWidth, screenHeight)

        // Only render 2 cards: front card + next card (next is full screen, hides others)
        val visibleCardCount = minOf(2, pageCount - swipeState.currentIndex)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .swipeableCard(
                        state = swipeState,
                        enabled = !swipeState.isAnimating && visibleCardCount > 0,
                        verticalSwipeEnabled = false,
                        onSwipeCommitted = { direction ->
                            // Video is already paused via isTouching/isDragging during drag
                        },
                        onSwipeComplete = { direction ->
                            // Animation finished - ensure video plays and notify
                            isPause = false
                            onPageLoaded(swipeState.currentIndex)
                            // Trigger vote callback for left/right swipes
                            if (direction == SwipeDirection.LEFT || direction == SwipeDirection.RIGHT) {
                                // currentIndex has already advanced, so previous card was at currentIndex - 1
                                val votedCardIndex = swipeState.currentIndex - 1
                                if (votedCardIndex >= 0) {
                                    onSwipeVote?.invoke(direction, votedCardIndex)
                                }
                            }
                        },
                        onEdgeReached = { direction ->
                            val reelDirection =
                                when (direction) {
                                    SwipeDirection.UP -> ReelScrollDirection.Up
                                    SwipeDirection.DOWN -> ReelScrollDirection.Down
                                    SwipeDirection.LEFT -> ReelScrollDirection.Left
                                    SwipeDirection.RIGHT -> ReelScrollDirection.Right
                                    SwipeDirection.NONE -> ReelScrollDirection.Up
                                }
                            onEdgeScrollAttempt(swipeState.currentIndex, false, reelDirection)
                        },
                    ),
        ) {
            // Render cards from back to front
            for (stackOffset in (visibleCardCount - 1) downTo 0) {
                val reelIndex = swipeState.currentIndex + stackOffset
                if (reelIndex >= reels.size) continue

                val reel = reels[reelIndex]
                val isFrontCard = stackOffset == 0

                // Both front and next card always have video players to avoid add/remove issues
                val isNextCard = stackOffset == 1
                // Always show video player on front and next cards
                val showVideoPlayer = isFrontCard || isNextCard

                // Get stored position for this video (for resuming after transition)
                val storedPosition = videoPositions[reel.videoId]

                // Key by videoId ensures Compose doesn't mix up cards during transitions
                key(reel.videoId) {
                    CardStackItem(
                        stackIndex = stackOffset,
                        playerData = reel.toPlayerData(reels, reelIndex),
                        playerConfig = playerConfig,
                        playerControls =
                            PlayerControls(
                                isPause =
                                    when {
                                        // Front card: pause when threshold crossed
                                        isFrontCard -> isPause || (swipeProgress >= CardStackConstants.EARLY_PLAYBACK_THRESHOLD)
                                        // Next card: play only when threshold crossed
                                        isNextCard -> swipeProgress < CardStackConstants.EARLY_PLAYBACK_THRESHOLD
                                        else -> true
                                    },
                                isMuted = !isFrontCard, // Mute next card to avoid audio overlap
                                onPauseToggle = { if (isFrontCard) isPause = !isPause },
                                recordTime = { currentTime, totalTime ->
                                    // Store position for this video
                                    videoPositions[reel.videoId] = currentTime
                                    // Only call the original callback for front card
                                    if (isFrontCard) recordTime(currentTime, totalTime)
                                },
                                initialSeekPosition = storedPosition,
                            ),
                        playerPool = playerPool,
                        videoListener = getVideoListener(reel),
                        swipeState = swipeState,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        showVideoPlayer = showVideoPlayer,
                        modifier = Modifier.fillMaxSize(),
                        overlayContent = {
                            // Always render overlay content so it's pre-loaded for next card
                            overlayContent(reelIndex) {
                                autoScrollToNext = true
                            }
                        },
                    )
                }
            }
        }

        // SwipeButtons placed OUTSIDE the swipeable Box so they can receive clicks
        // Buttons scale up and hide based on swipe direction
        SwipeButtons(
            onFlopClick = {
                if (!swipeState.isAnimating) {
                    val votedCardIndex = swipeState.currentIndex
                    coroutineScope.launch {
                        swipeState.swipeInDirection(
                            direction = SwipeDirection.LEFT,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            onComplete = {
                                isPause = false
                                onPageLoaded(swipeState.currentIndex)
                                onSwipeVote?.invoke(SwipeDirection.LEFT, votedCardIndex)
                            },
                        )
                    }
                }
            },
            onHitClick = {
                if (!swipeState.isAnimating) {
                    val votedCardIndex = swipeState.currentIndex
                    coroutineScope.launch {
                        swipeState.swipeInDirection(
                            direction = SwipeDirection.RIGHT,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            onComplete = {
                                isPause = false
                                onPageLoaded(swipeState.currentIndex)
                                onSwipeVote?.invoke(SwipeDirection.RIGHT, votedCardIndex)
                            },
                        )
                    }
                }
            },
            swipeDirection = swipeState.swipeDirection,
            swipeProgress = swipeState.calculateSwipeProgress(screenWidth, screenHeight),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 50.dp),
        )
    }

    // Handle auto scroll to next (triggered by overlay)
    LaunchedEffect(autoScrollToNext) {
        if (autoScrollToNext && !swipeState.isAtEnd()) {
            swipeState.advanceToNext()
            onPageLoaded(swipeState.currentIndex)
            autoScrollToNext = false
        } else {
            autoScrollToNext = false
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
