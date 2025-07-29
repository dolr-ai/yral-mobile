package com.yral.android.ui.screens.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.yral.android.R
import com.yral.android.ui.widgets.YralFeedback
import com.yral.shared.features.game.domain.models.GameIcon

@Composable
fun GameIconStrip(
    modifier: Modifier,
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    onIconClicked: (GameIcon) -> Unit,
    isLoading: Boolean,
    coinDelta: Int = 0,
    onIconPositioned: (Int, Float) -> Unit = { _, _ -> },
    currentIcon: Int? = null,
    onStripAnimationComplete: () -> Unit = {},
) {
    var playSound by remember { mutableStateOf(false) }
    var animatingIcon by remember(clickedIcon) { mutableStateOf(clickedIcon) }
    LaunchedEffect(currentIcon) {
        currentIcon?.takeIf { it in gameIcons.indices }?.let { index ->
            animatingIcon = gameIcons[index]
        }
    }
    GameStripBackground(modifier, currentIcon != null) {
        gameIcons.forEachIndexed { index, icon ->
            val shouldAnimate = animatingIcon?.id == icon.id
            var loadLocal by remember(icon.imageUrl) { mutableStateOf(false) }
            LaunchedEffect(icon.imageUrl) {
                if (icon.imageUrl.isEmpty() && !loadLocal) loadLocal = true
            }

            val clickableModifier =
                Modifier.clickable(
                    enabled = coinDelta == 0 && !isLoading && currentIcon == null,
                    onClick = {
                        if (coinDelta == 0 && !isLoading) {
                            playSound = true
                            onIconClicked(icon)
                        }
                    },
                )

            Box(
                modifier =
                    Modifier.onGloballyPositioned { coordinates ->
                        onIconPositioned(index, coordinates.positionInParent().x)
                    },
            ) {
                if (loadLocal) {
                    LocalGameIcon(
                        modifier = clickableModifier,
                        icon = icon,
                        animate = shouldAnimate,
                        onAnimationComplete = {
                            animatingIcon = null
                            onStripAnimationComplete()
                        },
                    )
                } else {
                    AsyncGameIcon(
                        modifier = clickableModifier,
                        icon = icon,
                        animate = shouldAnimate,
                        loadLocal = { loadLocal = true },
                        onAnimationComplete = {
                            animatingIcon = null
                            onStripAnimationComplete()
                        },
                    )
                }
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
            sound = R.raw.pop_pressed,
            withHapticFeedback = true,
            onPlayed = { setPlaySound(false) },
        )
    }
}
