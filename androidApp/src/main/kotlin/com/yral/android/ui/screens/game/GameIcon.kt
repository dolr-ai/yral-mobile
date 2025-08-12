package com.yral.android.ui.screens.game

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.yral.android.R
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames

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
    if (iconResource != 0) {
        Image(
            modifier = modifier,
            painter = painterResource(id = iconResource),
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
                style =
                    TextStyle(
                        platformStyle =
                            PlatformTextStyle(
                                emojiSupportMatch = EmojiSupportMatch.Default,
                            ),
                    ),
            )
        }
    }
}

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

fun GameIcon.getResource(): Int =
    when (imageName) {
        GameIconNames.LAUGH -> R.drawable.laughing
        GameIconNames.HEART -> R.drawable.heart
        GameIconNames.FIRE -> R.drawable.fire
        GameIconNames.SURPRISE -> R.drawable.surprise
        GameIconNames.ROCKET -> R.drawable.rocket
        GameIconNames.PUKE -> R.drawable.puke
        GameIconNames.UNKNOWN -> 0
    }
