package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.cardstack.ReelSwipeableCardStack
import com.yral.shared.libs.videoPlayer.cardstack.SwipeDirection
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.util.ReelScrollDirection

/**
 * A Tinder-style swipeable card stack video player for reels.
 *
 * This is a drop-in replacement for [YRALReelPlayer] that displays videos
 * in a stacked card layout. Users can swipe in any direction (up, down, left, right)
 * to dismiss the current video and reveal the next one.
 *
 * Features:
 * - Multi-directional swipe gestures
 * - 3 cards visible in the stack behind the current card
 * - Color tint feedback during swipe
 * - Stack promotion animation when card is dismissed
 * - Full screen video playback
 *
 * @param modifier Modifier for the component.
 * @param reels List of video reels to display.
 * @param maxReelsInPager Maximum number of reels to render.
 * @param initialPage Initial page index to display.
 * @param onPageLoaded Callback when a new page becomes the active card.
 * @param recordTime Callback to record viewing time (currentTime, totalTime).
 * @param didVideoEnd Callback when the current video ends.
 * @param onEdgeScrollAttempt Callback when user tries to swipe past the last video.
 * @param getPrefetchListener Factory for creating prefetch listeners per reel.
 * @param overlayContent Content to overlay on each video card (UI controls, etc.).
 * @param onSwipeVote Callback when a swipe vote is registered (direction, pageIndex).
 */
@Composable
fun YRALReelPlayerCardStack(
    modifier: Modifier = Modifier,
    reels: List<Reels>,
    maxReelsInPager: Int,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    didVideoEnd: () -> Unit,
    onEdgeScrollAttempt: (pageNo: Int, atStart: Boolean, direction: ReelScrollDirection) -> Unit = { _, _, _ -> },
    onSwipeVote: ((direction: SwipeDirection, pageIndex: Int) -> Unit)? = null,
    overlayContent: @Composable (pageNo: Int, scrollToNext: () -> Unit) -> Unit,
) {
    ReelSwipeableCardStack(
        modifier = modifier.fillMaxSize(),
        reels = reels,
        maxReelsInPager = maxReelsInPager,
        initialPage = initialPage,
        onPageLoaded = onPageLoaded,
        recordTime = recordTime,
        didVideoEnd = didVideoEnd,
        onEdgeScrollAttempt = onEdgeScrollAttempt,
        overlayContent = overlayContent,
        onSwipeVote = onSwipeVote,
    )
}
