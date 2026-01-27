package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yral.shared.libs.videoPlayer.model.Platform
import com.yral.shared.libs.videoPlayer.util.isPlatform

/**
 * Layout wrapper for a single card in the stack.
 * Applies stack transforms, padding, rounding, and shadow based on position.
 */
@Suppress("LongMethod")
@Composable
internal fun SwipeableCardStackItem(
    stackIndex: Int,
    state: SwipeableCardState,
    screenWidth: Float,
    screenHeight: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val isFrontCard = stackIndex == 0
    val isNextCard = stackIndex == 1

    val touchBaseProgress = if (state.isTouching) CardStackConstants.TOUCH_BASE_PROGRESS else 0f
    val dragProgress = state.calculateSwipeProgress(screenWidth, screenHeight).coerceIn(0f, 1f)
    val visualProgress = maxOf(touchBaseProgress, dragProgress).coerceIn(0f, 1f)

    val cardTransforms =
        if (isFrontCard) {
            CardTransforms(
                offsetX = state.offsetX,
                offsetY = state.offsetY,
                rotation = state.rotation,
                scale = 1f,
            )
        } else if (isNextCard) {
            CardTransforms(
                offsetX = 0f,
                offsetY = 0f,
                rotation = 0f,
                scale = 1f,
            )
        } else {
            val targetScale = 1f - (stackIndex * CardStackConstants.CARD_SCALE_STEP)
            val targetOffsetY =
                with(density) {
                    -(stackIndex * CardStackConstants.CARD_OFFSET_STEP_DP).dp.toPx()
                }

            val currentScale = 1f - ((1f - targetScale) * visualProgress)
            val currentOffsetY = targetOffsetY * visualProgress

            CardTransforms(
                offsetX = 0f,
                offsetY = currentOffsetY,
                rotation = 0f,
                scale = currentScale,
            )
        }

    val zIndex = (CardStackConstants.VISIBLE_CARDS + 1 - stackIndex).toFloat()

    val shadowElevation =
        if (isNextCard) {
            0f
        } else {
            CardStackConstants.CARD_SHADOW_ELEVATION_DP * (1f - stackIndex * CardStackConstants.SHADOW_ELEVATION_DECAY)
        }

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

    val isIos = isPlatform() == Platform.Ios
    val dynamicCardShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
                .zIndex(zIndex)
                .graphicsLayer {
                    compositingStrategy = if (isIos) CompositingStrategy.Auto else CompositingStrategy.Offscreen
                    translationX = cardTransforms.offsetX
                    translationY = cardTransforms.offsetY
                    rotationZ = cardTransforms.rotation
                    scaleX = cardTransforms.scale
                    scaleY = cardTransforms.scale
                    shape = dynamicCardShape
                    clip = true
                    this.shadowElevation = shadowElevation
                },
        contentAlignment = Alignment.TopStart,
    ) {
        content()
    }
}

private data class CardTransforms(
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float,
    val scale: Float,
)
