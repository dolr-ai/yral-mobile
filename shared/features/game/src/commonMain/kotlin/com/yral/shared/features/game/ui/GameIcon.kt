package com.yral.shared.features.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.libs.designsystem.component.YralAsyncImage

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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = icon.unicode,
            maxLines = 1,
            style = emojiTextStyle().copy(fontSize = 28.sp),
        )
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
