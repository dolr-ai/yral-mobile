package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 * @param didVideoEnd Callback when the current video ends.
 * @param onEdgeScrollAttempt Callback when user tries to swipe past the end.
 * @param getPrefetchListener Factory for prefetch listeners.
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

    val reporter =
        rememberPlaybackEventReporter(
            didVideoEnd = didVideoEnd,
            recordTime = recordTime,
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

    // Trigger scroll to next (for overlay button)
    var autoScrollToNext by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val coroutineScope = rememberCoroutineScope()

        @Suppress("MagicNumber")
        val scrollHintThreshold = 0.15f

        LaunchedEffect(swipeState, mediaItems.size, screenWidth, screenHeight, coordinator) {
            snapshotFlow {
                val progress = swipeState.calculateSwipeProgress(screenWidth, screenHeight)
                val shouldHint =
                    swipeState.swipeDirection != SwipeDirection.NONE && progress >= scrollHintThreshold
                swipeState.currentIndex to shouldHint
            }.distinctUntilChanged()
                .collect { (index, shouldHint) ->
                    val predicted = index + 1
                    if (shouldHint && predicted in 0 until mediaItems.size) {
                        coordinator.setScrollHint(predictedIndex = predicted, velocity = null)
                    }
                }
        }

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
                if (reelIndex >= visibleReels.size) continue

                val reel = visibleReels[reelIndex]

                // Key by videoId ensures Compose doesn't mix up cards during transitions
                key(reel.videoId) {
                    CardStackItem(
                        stackIndex = stackOffset,
                        playerData = reel.toPlayerData(visibleReels, reelIndex),
                        coordinator = coordinator,
                        mediaIndex = reelIndex,
                        swipeState = swipeState,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
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
): PlaybackEventReporter =
    remember(didVideoEnd, recordTime) {
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
            ) = Unit

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
