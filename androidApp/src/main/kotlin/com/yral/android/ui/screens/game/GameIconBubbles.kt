package com.yral.android.ui.screens.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.yral.android.ui.widgets.YralRemoteLottieAnimation
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation

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
            contentScale = ContentScale.Inside,
            iterations = 1,
            onAnimationComplete = onAnimationComplete,
            onError = { error -> playLocalAnimation = true },
        )
    } else {
        val animationRes = icon.getBubbleResource()
        if (animationRes != null) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = animationRes,
                iterations = 1,
                contentScale = ContentScale.Inside,
                onAnimationComplete = onAnimationComplete,
            )
        }
    }
}

fun GameIcon.getBubbleResource(): LottieRes? =
    when (imageName) {
        GameIconNames.LAUGH -> LottieRes.SMILEY_GAME_LAUGH
        GameIconNames.HEART -> LottieRes.SMILEY_GAME_HEART
        GameIconNames.FIRE -> LottieRes.SMILEY_GAME_FIRE
        GameIconNames.SURPRISE -> LottieRes.SMILEY_GAME_SURPRISE
        GameIconNames.ROCKET -> LottieRes.SMILEY_GAME_ROCKET
        GameIconNames.PUKE -> LottieRes.SMILEY_GAME_PUKE
        GameIconNames.UNKNOWN -> null
    }
