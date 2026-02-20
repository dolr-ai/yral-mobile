package com.yral.shared.libs.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import yral_mobile.shared.libs.designsystem.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun YralGifImage(
    resPath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    shape: Shape = CircleShape,
) {
    var bytes by remember(resPath) { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(resPath) {
        isLoading = true
        bytes = Res.readBytes(resPath)
        isLoading = false
    }
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        bytes?.let {
            YralGifImageFromBytes(bytes = it, modifier = modifier, contentScale = contentScale)
        }
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(maxWidth)
                        .height(maxHeight)
                        .clip(shape)
                        .shimmer(cornerRadius = 4.dp),
            )
        }
    }
}

@Composable
internal expect fun YralGifImageFromBytes(
    bytes: ByteArray,
    modifier: Modifier,
    contentScale: ContentScale,
)
