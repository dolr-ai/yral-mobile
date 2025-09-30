package com.yral.shared.libs.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.DrawableResource

@Composable
actual fun YralMaskedVectorTextV2(
    text: String,
    drawableRes: DrawableResource,
    textStyle: TextStyle,
    modifier: Modifier,
    maxLines: Int,
    textOverflow: TextOverflow,
) {
    Text(
        text = text,
        style = textStyle,
        modifier = modifier,
        maxLines = maxLines,
        overflow = textOverflow,
    )
}
