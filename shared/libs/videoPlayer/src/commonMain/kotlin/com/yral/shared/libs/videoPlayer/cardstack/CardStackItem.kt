package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.ui.VideoSurfaceSlot
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.videoplayer.generated.resources.Res
import yral_mobile.shared.libs.videoplayer.generated.resources.bakwaas
import yral_mobile.shared.libs.videoplayer.generated.resources.mast

/**
 * A single card item in the card stack.
 * Handles the visual transformation (scale, offset, rotation) based on its position in the stack.
 *
 * @param stackIndex Position in the stack (0 = front card, 1+ = stacked behind).
 * @param playerData Data for the video player.
 * @param coordinator Playback coordinator for video surfaces.
 * @param mediaIndex Index of this media item in the coordinator feed.
 * @param swipeState State holder for swipe gestures (only used by front card).
 * @param screenWidth Width of the screen for calculating transforms.
 * @param screenHeight Height of the screen for calculating transforms.
 * @param showVideoPlayer Whether to render the video surface for this card.
 * @param modifier Modifier for the card.
 * @param overlayContent Content to overlay on the video (UI controls, etc.).
 */
@Suppress("LongMethod")
@Composable
internal fun CardStackItem(
    stackIndex: Int,
    playerData: PlayerData,
    coordinator: PlaybackCoordinator,
    mediaIndex: Int,
    swipeState: SwipeableCardState,
    screenWidth: Float,
    screenHeight: Float,
    modifier: Modifier = Modifier,
    overlayContent: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val isFrontCard = stackIndex == 0
    val isNextCard = stackIndex == 1

    // Calculate visual progress: show card stack on touch, increase on drag
    // Base progress when touching (shows initial card pop-out effect)
    val touchBaseProgress = if (swipeState.isTouching) CardStackConstants.TOUCH_BASE_PROGRESS else 0f
    val dragProgress = swipeState.calculateSwipeProgress(screenWidth, screenHeight).coerceIn(0f, 1f)
    // Combined progress: touch gives base visibility, drag adds more
    val visualProgress = maxOf(touchBaseProgress, dragProgress).coerceIn(0f, 1f)

    // For front card, apply drag transforms
    // For next card, keep full screen (no transforms) so it appears ready
    // For other cards, apply stacked appearance
    val cardTransforms =
        if (isFrontCard) {
            CardTransforms(
                offsetX = swipeState.offsetX,
                offsetY = swipeState.offsetY,
                rotation = swipeState.rotation,
                scale = 1f,
            )
        } else if (isNextCard) {
            // Next card is full screen, no transforms
            CardTransforms(
                offsetX = 0f,
                offsetY = 0f,
                rotation = 0f,
                scale = 1f,
            )
        } else {
            // Cards start hidden (scale=1, offset=0) and reveal based on visual progress
            // Target values when fully revealed
            val targetScale = 1f - (stackIndex * CardStackConstants.CARD_SCALE_STEP)
            val targetOffsetY =
                with(density) {
                    -(stackIndex * CardStackConstants.CARD_OFFSET_STEP_DP).dp.toPx()
                }

            // Interpolate from hidden (1f, 0f) to revealed (targetScale, targetOffsetY)
            val currentScale = 1f - ((1f - targetScale) * visualProgress)
            val currentOffsetY = targetOffsetY * visualProgress

            CardTransforms(
                offsetX = 0f,
                offsetY = currentOffsetY,
                rotation = 0f,
                scale = currentScale,
            )
        }

    // Z-index: front card on top
    val zIndex = (CardStackConstants.VISIBLE_CARDS + 1 - stackIndex).toFloat()

    // Shadow elevation decreases for cards further back (none for next card)
    val shadowElevation =
        if (isNextCard) {
            0f
        } else {
            CardStackConstants.CARD_SHADOW_ELEVATION_DP * (1f - stackIndex * CardStackConstants.SHADOW_ELEVATION_DECAY)
        }

    // Front card: no padding/rounding when idle, reveal card style based on visual progress
    // Next card: full screen, no padding/rounding
    val horizontalPadding =
        if (isFrontCard) {
            (CardStackConstants.CARD_HORIZONTAL_PADDING_DP * visualProgress).dp
        } else if (isNextCard) {
            0.dp
        } else {
            CardStackConstants.CARD_HORIZONTAL_PADDING_DP.dp
        }

    val cornerRadius =
        if (isFrontCard) {
            (CardStackConstants.CARD_CORNER_RADIUS_DP * visualProgress).dp
        } else if (isNextCard) {
            0.dp
        } else {
            CardStackConstants.CARD_CORNER_RADIUS_DP.dp
        }

    val dynamicCardShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
                .zIndex(zIndex)
                .graphicsLayer {
                    // Use Offscreen compositing to ensure video surface transforms with content
                    compositingStrategy = CompositingStrategy.Offscreen
                    translationX = cardTransforms.offsetX
                    translationY = cardTransforms.offsetY
                    rotationZ = cardTransforms.rotation
                    scaleX = cardTransforms.scale
                    scaleY = cardTransforms.scale
                    // Apply shape clipping and shadow
                    shape = dynamicCardShape
                    clip = true
                    this.shadowElevation = shadowElevation
                },
        contentAlignment = Alignment.TopStart,
    ) {
        key(playerData.videoId) {
            VideoSurfaceSlot(
                index = mediaIndex,
                coordinator = coordinator,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                shutter = {
                    AsyncImage(
                        model = playerData.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                },
            )
        }

        // Swipe feedback overlay for front card only
        if (isFrontCard) {
            // Swipe feedback overlay
            val progress = swipeState.calculateSwipeProgress(screenWidth, screenHeight)
            SwipeFeedbackOverlay(
                direction = swipeState.swipeDirection,
                progress = progress,
                modifier = Modifier.fillMaxSize(),
            )

            // Swipe animation icons - show based on swipe direction
            val swipeDirection = swipeState.swipeDirection
            val iconAlpha = (progress * 2f).coerceIn(0f, 1f) // Fade in as swipe progresses

            // MAST animation icon on TOP LEFT when swiping RIGHT (24dp below trophy icon)
            if (swipeDirection == SwipeDirection.RIGHT) {
                Image(
                    painter = painterResource(Res.drawable.mast),
                    contentDescription = "Mast Animation",
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 40.dp, top = 88.dp)
                            .size(width = 171.dp, height = 165.dp)
                            .alpha(iconAlpha),
                )
            }

            // BAKWAAS animation icon on TOP RIGHT when swiping LEFT (24dp below wallet icon)
            if (swipeDirection == SwipeDirection.LEFT) {
                Image(
                    painter = painterResource(Res.drawable.bakwaas),
                    contentDescription = "Bakwaas Animation",
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 40.dp, top = 88.dp)
                            .size(width = 206.dp, height = 115.dp)
                            .alpha(iconAlpha),
                )
            }
        }

        // UI overlay content - show on all cards for pre-loading
        overlayContent()
    }
}

/**
 * Data class holding transform values for a card.
 */
private data class CardTransforms(
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float,
    val scale: Float,
)
