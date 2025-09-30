package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.DrawableResource

@Composable
expect fun YralMaskedVectorTextV2(
    text: String,
    drawableRes: DrawableResource,
    textStyle: TextStyle,
    modifier: Modifier = Modifier, // width need to specified according to useCase
    maxLines: Int = Int.MAX_VALUE,
    textOverflow: TextOverflow = TextOverflow.Clip,
)
