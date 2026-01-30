package com.yral.shared.features.game.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.libs.designsystem.component.lottie.YralRemoteLottieAnimation

@Composable
fun GameIconBubbles(
    icon: GameIcon,
    onAnimationComplete: () -> Unit,
) {
    var playLocalAnimation by remember {
        mutableStateOf(icon.clickAnimation.isEmpty())
        // mutableStateOf(true)
    }
    if (!playLocalAnimation) {
        YralRemoteLottieAnimation(
            modifier = Modifier.fillMaxSize(),
            url = icon.clickAnimation,
            contentScale = ContentScale.FillBounds,
            iterations = 1,
            onAnimationComplete = onAnimationComplete,
            onError = { error -> playLocalAnimation = true },
        )
    } else if (icon.unicode.isNotEmpty()) {
        // Use unicode emoji bubbles animation for all emojis
        EmojiBubblesAnimation(
            emoji = icon.unicode,
            modifier = Modifier.fillMaxSize(),
            onAnimationComplete = onAnimationComplete,
        )
    }
}
