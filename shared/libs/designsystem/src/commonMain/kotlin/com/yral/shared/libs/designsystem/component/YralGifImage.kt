package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.ExperimentalResourceApi
import yral_mobile.shared.libs.designsystem.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun YralGifImage(
    resPath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    var bytes by remember(resPath) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(resPath) {
        bytes = Res.readBytes(resPath)
    }
    bytes?.let {
        YralGifImageFromBytes(bytes = it, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
internal expect fun YralGifImageFromBytes(
    bytes: ByteArray,
    modifier: Modifier,
    contentScale: ContentScale,
)
