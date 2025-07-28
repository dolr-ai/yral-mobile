package com.yral.android.ui.screens.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import co.touchlab.kermit.Logger
import com.yral.android.R
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.android.ui.widgets.YralRemoteLottieAnimation
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames

@Composable
fun GameIconBubbles(
    icon: GameIcon,
    onAnimationComplete: () -> Unit,
) {
    var playLocalAnimation by remember {
        // mutableStateOf(icon.clickAnimation.isEmpty())
        mutableStateOf(true)
    }
    if (!playLocalAnimation) {
        YralRemoteLottieAnimation(
            modifier = Modifier.fillMaxSize(),
            url = icon.clickAnimation,
            contentScale = ContentScale.Inside,
            iterations = 1,
            onAnimationComplete = {
                onAnimationComplete()
                Logger.d("xxxx Lottie Animation completed")
            },
            onError = { error ->
                playLocalAnimation = true
                Logger.e("xxxx Lottie Error loading animation", error)
            },
            onLoading = {
                Logger.d("xxxx Lottie Loading animation...")
            },
        )
    } else {
        val animationRes = icon.getBubbleResource()
        YralLottieAnimation(
            modifier = Modifier.fillMaxSize(),
            rawRes = animationRes,
            iterations = 1,
            contentScale = ContentScale.Inside,
            onAnimationComplete = onAnimationComplete,
        )
    }
}

private fun GameIcon.getBubbleResource(): Int =
    when (imageName) {
        GameIconNames.LAUGH -> R.raw.smiley_game_laugh
        GameIconNames.HEART -> R.raw.smiley_game_heart
        GameIconNames.FIRE -> R.raw.smiley_game_fire
        GameIconNames.SURPRISE -> R.raw.smiley_game_surprise
        GameIconNames.ROCKET -> R.raw.smiley_game_rocket
    }
