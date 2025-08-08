package com.yral.android.ui.screens.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
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
        CenteredAutoResizeText(
            text = icon.unicode,
            textStyle =
                TextStyle(
                    platformStyle =
                        PlatformTextStyle(
                            emojiSupportMatch = EmojiSupportMatch.Default,
                        ),
                ),
            modifier = modifier,
        )
    }
}

@Composable
private fun CenteredAutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 12.sp,
    maxFontSize: TextUnit = 25.sp,
    stepSize: TextUnit = 1.sp,
    maxLines: Int = 1,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AutoResizeText(
            text = text,
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            stepSize = stepSize,
            maxLines = maxLines,
            textAlign = TextAlign.Center,
            textStyle = textStyle,
        )
    }
}

@Composable
private fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 12.sp,
    maxFontSize: TextUnit = 25.sp,
    stepSize: TextUnit = 1.sp,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    var fontSize by remember { mutableStateOf(maxFontSize) }
    var readyToDraw by remember { mutableStateOf(false) }
    Text(
        text = text,
        maxLines = maxLines,
        fontSize = fontSize,
        textAlign = textAlign,
        modifier =
            modifier.drawWithContent {
                if (readyToDraw) drawContent()
            },
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) {
                if (fontSize > minFontSize) {
                    fontSize = (fontSize.value - stepSize.value).sp // decrease font size and recompose
                }
            } else {
                readyToDraw = true // text fits, allow drawing
            }
        },
        style = textStyle.copy(fontSize = fontSize), // use passed style with dynamic fontSize
    )
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
