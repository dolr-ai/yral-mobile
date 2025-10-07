package com.yral.shared.features.game.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.ui.GameIconStrip.GAME_ICON_ANIMATION_DURATION
import com.yral.shared.features.game.ui.GameIconStrip.GAME_ICON_NUDGE_ANIMATION_DURATION
import com.yral.shared.features.game.ui.GameIconStrip.GAME_ICON_ROTATION_DEGREE
import com.yral.shared.features.game.ui.GameIconStrip.GAME_ICON_SCALING_FACTOR
import com.yral.shared.libs.designsystem.component.YralFeedback
import com.yral.shared.libs.designsystem.component.popPressedSoundId
import kotlinx.coroutines.delay

@Suppress("LongMethod")
@Composable
fun GameIconStrip(
    modifier: Modifier,
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    onIconClicked: (GameIcon) -> Unit,
    isLoading: Boolean,
    coinDelta: Int = 0,
    onIconPositioned: (Int, Float) -> Unit = { _, _ -> },
    animatingNudgeIconPosition: Int? = null,
    onIconAnimationComplete: () -> Unit = {},
    setNudgeShown: () -> Unit = {},
) {
    var playSound by remember { mutableStateOf(false) }
    var animatingIcon by remember(clickedIcon) { mutableStateOf(clickedIcon) }
    LaunchedEffect(animatingNudgeIconPosition) {
        animatingNudgeIconPosition?.takeIf { it in gameIcons.indices }?.let { index ->
            animatingIcon = gameIcons[index]
        }
    }
    GameStripBackground(modifier, animatingNudgeIconPosition != null) {
        gameIcons.forEachIndexed { index, icon ->
            val shouldAnimate = animatingIcon?.id == icon.id
            val clickableModifier =
                Modifier.clickable(
                    enabled = coinDelta == 0 && !isLoading,
                    onClick = {
                        if (coinDelta == 0 && !isLoading) {
                            playSound = true
                            onIconClicked(icon)
                            setNudgeShown()
                        }
                    },
                )

            Box(
                modifier =
                    Modifier.onGloballyPositioned { coordinates ->
                        onIconPositioned(index, coordinates.positionInParent().x)
                    },
            ) {
                val animationDuration =
                    animatingNudgeIconPosition?.let { GAME_ICON_ANIMATION_DURATION }
                        ?: GAME_ICON_NUDGE_ANIMATION_DURATION
                val rotation by animateFloatAsState(
                    targetValue = if (shouldAnimate) GAME_ICON_ROTATION_DEGREE else 0f,
                    animationSpec = tween(durationMillis = animationDuration.toInt()),
                    label = "rotation",
                )
                val scale by animateFloatAsState(
                    targetValue = if (shouldAnimate) GAME_ICON_SCALING_FACTOR else 1f,
                    animationSpec = tween(durationMillis = animationDuration.toInt()),
                    label = "scale",
                )
                LaunchedEffect(shouldAnimate) {
                    if (shouldAnimate) {
                        delay(animationDuration)
                        animatingIcon = null
                        onIconAnimationComplete()
                    }
                }
                GameIcon(
                    modifier =
                        clickableModifier
                            .size(46.dp)
                            .graphicsLayer(
                                rotationZ = rotation,
                                scaleX = scale,
                                scaleY = scale,
                            ),
                    icon = icon,
                )
            }
        }
    }
    Feedback(playSound) { playSound = it }
}

@Composable
private fun Feedback(
    playSound: Boolean,
    setPlaySound: (Boolean) -> Unit,
) {
    if (playSound) {
        YralFeedback(
            sound = popPressedSoundId(),
            withHapticFeedback = true,
            onPlayed = { setPlaySound(false) },
        )
    }
}

private object GameIconStrip {
    const val GAME_ICON_ROTATION_DEGREE = -15f
    const val GAME_ICON_SCALING_FACTOR = 1.17f
    const val GAME_ICON_ANIMATION_DURATION = 200L
    const val GAME_ICON_NUDGE_ANIMATION_DURATION = 360L
}
