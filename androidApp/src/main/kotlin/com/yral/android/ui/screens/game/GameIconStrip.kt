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
import com.yral.android.ui.widgets.YralPlaySound
import com.yral.shared.features.game.domain.models.GameIcon

@Composable
fun GameIconStrip(
    modifier: Modifier,
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    onIconClicked: (emoji: GameIcon) -> Unit,
    isLoading: Boolean,
    coinDelta: Int = 0,
    onIconPositioned: (Int, Float) -> Unit = { _, _ -> },
) {
    var playSound by remember { mutableStateOf(false) }
    var animateIcon by remember { mutableStateOf(false) }
    GameStripBackground(modifier) {
        gameIcons.forEachIndexed { index, icon ->
            var loadLocal by remember { mutableStateOf(false) }
            LaunchedEffect(icon.imageUrl) {
                if (icon.imageUrl.isEmpty() && !loadLocal) {
                    loadLocal = true
                }
            }
            Box(
                modifier =
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            onIconPositioned(index, coordinates.positionInParent().x)
                        },
            ) {
                if (loadLocal) {
                    LocalGameIcon(
                        modifier =
                            Modifier.clickable {
                                if (coinDelta == 0 && !isLoading) {
                                    animateIcon = true
                                    playSound = true
                                    onIconClicked(icon)
                                }
                            },
                        icon = icon,
                        animate = if (clickedIcon?.id == icon.id) animateIcon else false,
                        onAnimationComplete = { animateIcon = false },
                    )
                } else {
                    AsyncGameIcon(
                        modifier =
                            Modifier.clickable {
                                if (coinDelta == 0 && !isLoading) {
                                    animateIcon = true
                                    playSound = true
                                    onIconClicked(icon)
                                }
                            },
                        icon = icon,
                        animate = if (clickedIcon?.id == icon.id) animateIcon else false,
                        onAnimationComplete = { animateIcon = false },
                        loadLocal = { loadLocal = true },
                    )
                }
            }
        }
    }
    if (playSound) {
        YralPlaySound(sound = R.raw.pop_pressed) { playSound = false }
    }
}
