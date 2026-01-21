package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.model.toPlayerData
import com.yral.shared.libs.videoPlayer.util.ReelScrollDirection
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.PlaybackEventReporter
import com.yral.shared.libs.videoplayback.ui.VideoFeedSync
import com.yral.shared.libs.videoplayback.ui.rememberPlaybackCoordinatorWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Reel-specific card stack that wires video playback into the generic stack.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun ReelSwipeableCardStack(
    modifier: Modifier = Modifier,
    reels: List<Reels>,
    maxReelsInPager: Int,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    didVideoEnd: () -> Unit,
    onEdgeScrollAttempt: (pageNo: Int, atStart: Boolean, direction: ReelScrollDirection) -> Unit,
    overlayContent: @Composable (pageNo: Int, scrollToNext: () -> Unit) -> Unit,
    onSwipeVote: ((direction: SwipeDirection, pageIndex: Int) -> Unit)? = null,
) {
    val pageCount = minOf(reels.size, maxReelsInPager)
    if (pageCount == 0) return
    val visibleReels = remember(reels, pageCount) { reels.take(pageCount) }
    val mediaItems =
        remember(visibleReels) {
            visibleReels.map { reel ->
                MediaDescriptor(
                    id = reel.videoId,
                    uri = reel.videoUrl,
                )
            }
        }

    val swipeState =
        rememberSwipeableCardState(
            initialIndex = initialPage.coerceAtMost(pageCount - 1),
            itemCount = pageCount,
        )

    LaunchedEffect(pageCount) {
        swipeState.updateItemCount(pageCount)
    }

    var activeFrameReadyIndex by remember { mutableStateOf<Int?>(null) }

    val reporter =
        rememberPlaybackEventReporter(
            didVideoEnd = didVideoEnd,
            recordTime = recordTime,
            onFirstFrameRendered = { index ->
                if (index >= 0 && index == swipeState.currentIndex) {
                    activeFrameReadyIndex = index
                }
            },
        )
    val coordinator =
        rememberPlaybackCoordinatorWithLifecycle(
            deps = CoordinatorDeps(reporter = reporter),
        )
    VideoFeedSync(items = mediaItems, coordinator = coordinator)

    LaunchedEffect(swipeState, mediaItems.size, coordinator) {
        snapshotFlow { swipeState.currentIndex }
            .distinctUntilChanged()
            .collect { index ->
                activeFrameReadyIndex = null
                if (index in 0 until mediaItems.size) {
                    coordinator.setActiveIndex(index)
                }
            }
    }

    LaunchedEffect(swipeState, visibleReels) {
        snapshotFlow { swipeState.currentIndex }
            .distinctUntilChanged()
            .collect { page ->
                onPageLoaded(page)
            }
    }

    var autoScrollToNext by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val coroutineScope = rememberCoroutineScope()
        val isTransitioning = swipeState.currentIndex != swipeState.settledIndex

        @Suppress("MagicNumber")
        val scrollHintThreshold = 0.15f

        LaunchedEffect(swipeState, mediaItems.size, screenWidth, screenHeight, coordinator) {
            snapshotFlow {
                val progress = swipeState.calculateSwipeProgress(screenWidth, screenHeight)
                val shouldHint =
                    swipeState.swipeDirection != SwipeDirection.NONE && progress >= scrollHintThreshold
                swipeState.settledIndex to shouldHint
            }.distinctUntilChanged()
                .collect { (index, shouldHint) ->
                    val predicted = index + 1
                    if (shouldHint && predicted in 0 until mediaItems.size) {
                        coordinator.setScrollHint(predictedIndex = predicted, velocity = null)
                    }
                }
        }

        SwipeableCardStack(
            modifier = Modifier.fillMaxSize(),
            state = swipeState,
            count = pageCount,
            maxVisibleCards = 2,
            verticalSwipeEnabled = false,
            key = { index -> visibleReels[index].videoId },
            onSwipeComplete = { direction ->
                if (direction == SwipeDirection.LEFT || direction == SwipeDirection.RIGHT) {
                    val votedCardIndex = swipeState.settledIndex - 1
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
                onEdgeScrollAttempt(swipeState.settledIndex, false, reelDirection)
            },
        ) {
            val reelIndex = index
            if (reelIndex >= visibleReels.size) return@SwipeableCardStack

            val reel = visibleReels[reelIndex]

            ReelCardContent(
                playerData = reel.toPlayerData(visibleReels, reelIndex),
                coordinator = coordinator,
                mediaIndex = reelIndex,
                isFrontCard = isFrontCard,
                swipeDirection = swipeDirection,
                swipeProgress = swipeProgress,
                suppressShutter = reelIndex == activeFrameReadyIndex,
                showPlaceholderOverlay = reelIndex == swipeState.currentIndex && activeFrameReadyIndex != reelIndex,
                showSwipeOverlay = isFrontCard && !isTransitioning,
                modifier = Modifier.fillMaxSize(),
                overlayContent = {
                    overlayContent(reelIndex) {
                        autoScrollToNext = true
                    }
                },
            )
        }
        if (swipeState.currentIndex != swipeState.settledIndex) {
            val overlayIndex = swipeState.settledIndex
            val overlayReel = visibleReels.getOrNull(overlayIndex)
            if (overlayReel != null) {
                SwipeableCardStackItem(
                    stackIndex = 0,
                    state = swipeState,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    applyFrontTransform = true,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ReelCardContent(
                        playerData = overlayReel.toPlayerData(visibleReels, overlayIndex),
                        coordinator = coordinator,
                        mediaIndex = overlayIndex,
                        isFrontCard = true,
                        swipeDirection = swipeState.swipeDirection,
                        swipeProgress = swipeState.calculateSwipeProgress(screenWidth, screenHeight),
                        suppressShutter = true,
                        showPlaceholderOverlay = true,
                        showSwipeOverlay = true,
                        modifier = Modifier.fillMaxSize(),
                        overlayContent = { overlayContent(overlayIndex) {} },
                    )
                }
            }
        }



        SwipeButtons(
            onFlopClick = {
                if (!swipeState.isAnimating) {
                    val votedCardIndex = swipeState.settledIndex
                    coroutineScope.launch {
                        swipeState.swipeInDirection(
                            direction = SwipeDirection.LEFT,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            onComplete = {
                                onSwipeVote?.invoke(SwipeDirection.LEFT, votedCardIndex)
                            },
                        )
                    }
                }
            },
            onHitClick = {
                if (!swipeState.isAnimating) {
                    val votedCardIndex = swipeState.settledIndex
                    coroutineScope.launch {
                        swipeState.swipeInDirection(
                            direction = SwipeDirection.RIGHT,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            onComplete = {
                                onSwipeVote?.invoke(SwipeDirection.RIGHT, votedCardIndex)
                            },
                        )
                    }
                }
            },
            swipeDirection = if (swipeState.isDragging) swipeState.swipeDirection else SwipeDirection.NONE,
            swipeProgress =
                if (swipeState.isDragging) {
                    swipeState.calculateSwipeProgress(screenWidth, screenHeight)
                } else {
                    0f
                },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
                    .padding(bottom = 50.dp),
        )
    }

    LaunchedEffect(autoScrollToNext) {
        if (autoScrollToNext) {
            if (!swipeState.isAtEnd()) {
                swipeState.advanceToNext()
            }
            autoScrollToNext = false
        }
    }
}

@Composable
private fun rememberPlaybackEventReporter(
    didVideoEnd: () -> Unit,
    recordTime: (Int, Int) -> Unit,
    onFirstFrameRendered: (Int) -> Unit,
): PlaybackEventReporter =
    remember(didVideoEnd, recordTime, onFirstFrameRendered) {
        object : PlaybackEventReporter {
            override fun playbackEnded(
                id: String,
                index: Int,
            ) {
                didVideoEnd()
            }

            override fun playbackProgress(
                id: String,
                index: Int,
                positionMs: Long,
                durationMs: Long,
            ) {
                if (positionMs >= 0 && durationMs > 0) {
                    recordTime(positionMs.toInt(), durationMs.toInt())
                }
            }

            override fun feedItemImpression(
                id: String,
                index: Int,
            ) = Unit

            override fun playStartRequest(
                id: String,
                index: Int,
                reason: String,
            ) = Unit

            override fun firstFrameRendered(
                id: String,
                index: Int,
            ) {
                onFirstFrameRendered(index)
            }

            override fun timeToFirstFrame(
                id: String,
                index: Int,
                ms: Long,
            ) = Unit

            override fun rebufferStart(
                id: String,
                index: Int,
                reason: String,
            ) = Unit

            override fun rebufferEnd(
                id: String,
                index: Int,
                reason: String,
            ) = Unit

            override fun rebufferTotal(
                id: String,
                index: Int,
                ms: Long,
            ) = Unit

            override fun playbackError(
                id: String,
                index: Int,
                category: String,
                code: Any,
                message: String?,
            ) = Unit

            override fun preloadScheduled(
                id: String,
                index: Int,
                distance: Int,
                mode: String,
            ) = Unit

            override fun preloadCompleted(
                id: String,
                index: Int,
                bytes: Long,
                ms: Long,
                fromCache: Boolean,
            ) = Unit

            override fun preloadCanceled(
                id: String,
                index: Int,
                reason: String,
            ) = Unit

            override fun cacheHit(
                id: String,
                bytes: Long,
            ) = Unit

            override fun cacheMiss(
                id: String,
                bytes: Long,
            ) = Unit
        }
    }
