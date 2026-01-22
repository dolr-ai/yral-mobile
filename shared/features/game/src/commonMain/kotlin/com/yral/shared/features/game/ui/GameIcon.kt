package com.yral.shared.features.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.fire
import yral_mobile.shared.features.game.generated.resources.heart
import yral_mobile.shared.features.game.generated.resources.laughing
import yral_mobile.shared.features.game.generated.resources.puke
import yral_mobile.shared.features.game.generated.resources.rocket
import yral_mobile.shared.features.game.generated.resources.surprise

@Composable
fun GameIcon(
    modifier: Modifier,
    icon: GameIcon,
) {
    var loadLocal by remember(icon.imageUrl) { mutableStateOf(false) }
    LaunchedEffect(icon.imageUrl) {
        if (icon.imageUrl.isEmpty() && !loadLocal) loadLocal = true
    }
    if (loadLocal) {
        LocalGameIcon(
            modifier = modifier,
            icon = icon,
        )
    } else {
        AsyncGameIcon(
            modifier = modifier,
            icon = icon,
            loadLocal = { loadLocal = true },
        )
    }
}

@Composable
fun LocalGameIcon(
    modifier: Modifier,
    icon: GameIcon,
) {
    val iconResource = icon.getResource()
    if (iconResource != null) {
        Image(
            modifier = modifier,
            painter = painterResource(iconResource),
            contentDescription = "image description",
            contentScale = ContentScale.FillBounds,
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = icon.unicode,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(12.sp, 25.sp),
                style = emojiTextStyle(),
            )
        }
    }
}

internal expect fun emojiTextStyle(): TextStyle

@Composable
fun AsyncGameIcon(
    modifier: Modifier,
    icon: GameIcon,
    loadLocal: () -> Unit,
) {
    YralAsyncImage(
        imageUrl = icon.imageUrl,
        modifier = modifier,
        onError = { loadLocal() },
    )
}

fun GameIcon.getResource(): DrawableResource? =
    when (imageName) {
        GameIconNames.LAUGH -> Res.drawable.laughing
        GameIconNames.HEART -> Res.drawable.heart
        GameIconNames.FIRE -> Res.drawable.fire
        GameIconNames.SURPRISE -> Res.drawable.surprise
        GameIconNames.ROCKET -> Res.drawable.rocket
        GameIconNames.PUKE -> Res.drawable.puke
        GameIconNames.UNKNOWN -> getDrawableFromUnicode(unicode)
    }

/**
 * Maps common emoji unicode characters to existing drawable resources.
 * This enables dynamic emojis from Gemini to show prettier static images
 * when they match or are similar to our predefined emoji categories.
 */
private fun getDrawableFromUnicode(unicode: String): DrawableResource? =
    when (unicode) {
        // Laugh/Joy emojis
        "😂", "🤣", "😆", "😄", "😁", "😀", "😃", "😅", "😹", "🙂", "😊", "☺️" ->
            Res.drawable.laughing

        // Heart/Love emojis
        "❤️", "💕", "💖", "💗", "💓", "💞", "💘", "💝", "😍", "🥰", "😻", "💜", "💙", "💚",
        "🧡", "💛", "🤍", "🖤", "🤎", "💟", "❣️", "♥️", "🩷", "🩵", "🩶",
        ->
            Res.drawable.heart

        // Fire emojis
        "🔥", "💥", "⚡", "✨", "🌟", "⭐", "💫", "🎇", "🎆" ->
            Res.drawable.fire

        // Surprise/Shock emojis
        "😮", "😲", "🤯", "😱", "😨", "😧", "😦", "🙀", "😯", "😵", "🫢", "🫣", "😳" ->
            Res.drawable.surprise

        // Rocket/Speed/Achievement emojis
        "🚀", "🎯", "🏆", "🥇", "🎖️", "🏅", "💪", "👊", "✊", "🙌", "👏", "🎉", "🎊" ->
            Res.drawable.rocket

        // Puke/Disgust emojis
        "🤮", "🤢", "😷", "🤧", "😖", "😫", "😩", "💩", "👎", "😒", "😑", "😐" ->
            Res.drawable.puke

        // No matching image for other emojis - will render unicode text
        else -> null
    }
