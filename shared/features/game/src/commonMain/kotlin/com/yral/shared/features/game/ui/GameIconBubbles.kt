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
import com.yral.shared.features.game.domain.models.GameIconNames
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
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
    } else {
        val animationRes = icon.getBubbleResource()
        if (animationRes != null) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = animationRes,
                iterations = 1,
                contentScale = ContentScale.FillBounds,
                onAnimationComplete = onAnimationComplete,
            )
        } else if (icon.unicode.isNotEmpty()) {
            // For dynamic emojis without a Lottie animation, play the bubbles animation.
            // The scale/rotate animation on the icon itself is already handled by GameIconStrip.
            EmojiBubblesAnimation(
                emoji = icon.unicode,
                modifier = Modifier.fillMaxSize(),
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
        GameIconNames.UNKNOWN -> getLottieFromUnicode(unicode)
    }

/**
 * Maps common emoji unicode characters to existing Lottie animations.
 * This enables dynamic emojis from Gemini to have animations when they
 * match or are similar to our predefined emoji categories.
 */
private fun getLottieFromUnicode(unicode: String): LottieRes? =
    when (unicode) {
        // Laugh/Joy emojis
        "😂", "🤣", "😆", "😄", "😁", "😀", "😃", "😅", "😹", "🙂", "😊", "☺️" ->
            LottieRes.SMILEY_GAME_LAUGH

        // Heart/Love emojis
        "❤️", "💕", "💖", "💗", "💓", "💞", "💘", "💝", "😍", "🥰", "😻", "💜", "💙", "💚",
        "🧡", "💛", "🤍", "🖤", "🤎", "💟", "❣️", "♥️", "🩷", "🩵", "🩶",
        ->
            LottieRes.SMILEY_GAME_HEART

        // Fire emojis
        "🔥", "💥", "⚡", "✨", "🌟", "⭐", "💫", "🎇", "🎆" ->
            LottieRes.SMILEY_GAME_FIRE

        // Surprise/Shock emojis
        "😮", "😲", "🤯", "😱", "😨", "😧", "😦", "🙀", "😯", "😵", "🫢", "🫣", "😳" ->
            LottieRes.SMILEY_GAME_SURPRISE

        // Rocket/Speed/Achievement emojis
        "🚀", "🎯", "🏆", "🥇", "🎖️", "🏅", "💪", "👊", "✊", "🙌", "👏", "🎉", "🎊" ->
            LottieRes.SMILEY_GAME_ROCKET

        // Puke/Disgust emojis
        "🤮", "🤢", "😷", "🤧", "😖", "😫", "😩", "💩", "👎", "😒", "😑", "😐" ->
            LottieRes.SMILEY_GAME_PUKE

        // For all other emojis, return null to trigger custom EmojiBubblesAnimation
        else -> null
    }
