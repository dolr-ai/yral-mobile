package com.yral.shared.features.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.hon_swipe_left_instruction
import yral_mobile.shared.features.game.generated.resources.hon_swipe_right_instruction
import yral_mobile.shared.features.game.generated.resources.ic_hon_hand

private const val SCRIM_ALPHA = 0.5f
private const val HAND_ICON_SIZE = 64
private const val DASH_LENGTH = 10f
private const val GAP_LENGTH = 10f
private const val DASHED_LINE_WIDTH = 2f
private const val DASHED_LINE_VERTICAL_PADDING = 50
private const val ROTATION_DURATION_MS = 450
private const val ROTATION_ANGLE = 30f
private const val ANIMATION_LOOP_COUNT = 4

/**
 * Onboarding overlay for Hot or Not game mode.
 * Shows a split-screen overlay with swipe instructions:
 * - Left side: Swipe left for "BAKWASS"
 * - Right side: Swipe right for "MAST"
 *
 * Animation sequence:
 * 1. MAST (right) hand rotates clockwise to 30° and back to 0° - repeats 4 times
 * 2. BAKWASS (left) hand rotates anti-clockwise to -30° and back to 0° - repeats 4 times
 *
 * Auto-dismisses after animations complete. Also dismisses on tap anywhere.
 */
@Composable
fun HotOrNotOnboardingOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mastRotation = remember { Animatable(0f) }
    val bakwaasRotation = remember { Animatable(0f) }

    HandRotationAnimations(
        mastRotation = mastRotation,
        bakwaasRotation = bakwaasRotation,
        onAnimationComplete = onDismiss,
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
    ) {
        CenterDashedLine(modifier = Modifier.align(Alignment.Center))

        Row(modifier = Modifier.fillMaxSize()) {
            SwipeInstructionColumn(
                isLeft = true,
                rotationAngle = bakwaasRotation.value,
                modifier = Modifier.weight(1f),
            )
            SwipeInstructionColumn(
                isLeft = false,
                rotationAngle = mastRotation.value,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HandRotationAnimations(
    mastRotation: Animatable<Float, *>,
    bakwaasRotation: Animatable<Float, *>,
    onAnimationComplete: () -> Unit,
) {
    LaunchedEffect(Unit) {
        // First: MAST hand rotates 4 times (0° → 30° → 0°)
        repeat(ANIMATION_LOOP_COUNT) {
            mastRotation.animateTo(
                targetValue = ROTATION_ANGLE,
                animationSpec = tween(durationMillis = ROTATION_DURATION_MS, easing = LinearEasing),
            )
            mastRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = ROTATION_DURATION_MS, easing = LinearEasing),
            )
        }

        // Then: BAKWASS hand rotates 4 times (0° → -30° → 0°)
        repeat(ANIMATION_LOOP_COUNT) {
            bakwaasRotation.animateTo(
                targetValue = -ROTATION_ANGLE,
                animationSpec = tween(durationMillis = ROTATION_DURATION_MS, easing = LinearEasing),
            )
            bakwaasRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = ROTATION_DURATION_MS, easing = LinearEasing),
            )
        }

        onAnimationComplete()
    }
}

@Composable
private fun CenterDashedLine(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .width(DASHED_LINE_WIDTH.dp)
                .fillMaxHeight()
                .padding(vertical = DASHED_LINE_VERTICAL_PADDING.dp)
                .drawBehind {
                    val pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(DASH_LENGTH, GAP_LENGTH),
                            phase = 0f,
                        )
                    drawLine(
                        color = YralColors.Neutral500,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = DASHED_LINE_WIDTH,
                        pathEffect = pathEffect,
                    )
                },
    )
}

@Composable
private fun SwipeInstructionColumn(
    isLeft: Boolean,
    rotationAngle: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_hon_hand),
            contentDescription = if (isLeft) "Swipe left" else "Swipe right",
            modifier =
                Modifier
                    .size(HAND_ICON_SIZE.dp)
                    .graphicsLayer {
                        scaleX = if (isLeft) -1f else 1f
                        rotationZ = rotationAngle
                    },
        )

        Text(
            text =
                stringResource(
                    if (isLeft) {
                        Res.string.hon_swipe_left_instruction
                    } else {
                        Res.string.hon_swipe_right_instruction
                    },
                ),
            style =
                if (isLeft) {
                    LocalAppTopography.current.lgMedium
                } else {
                    LocalAppTopography.current.lgBold
                },
            color = YralColors.Neutral50,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}
