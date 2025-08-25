package com.yral.android.ui.widgets

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import kotlin.math.min

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun YralAsyncImage(
    imageUrl: Any,
    modifier: Modifier = Modifier,
    loaderSize: LoaderSize = LoaderSize.Percentage(),
    border: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    backgroundColor: Color = Color.Transparent,
    contentScale: ContentScale = ContentScale.FillBounds,
    shape: Shape = CircleShape,
    onError: () -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(true) }
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier =
                Modifier
                    .width(maxWidth)
                    .height(maxHeight)
                    .clip(shape)
                    .border(
                        width = border,
                        color = borderColor,
                        shape = shape,
                    ).background(
                        color = backgroundColor,
                        shape = shape,
                    ),
            contentScale = contentScale,
            model = imageUrl,
            contentDescription = "image",
            onState = {
                isLoading = it is AsyncImagePainter.State.Loading
                if (it is AsyncImagePainter.State.Error) {
                    onError()
                }
            },
        )
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val availableSize = min(maxWidth.value, maxHeight.value)
            val loaderSizeDp =
                when (loaderSize) {
                    is LoaderSize.None -> loaderSize.mSize
                    is LoaderSize.Fixed -> loaderSize.mSize
                    is LoaderSize.Dynamic -> loaderSize.size
                    is LoaderSize.Percentage -> (availableSize * loaderSize.percentage).dp
                }
            YralLoader(size = loaderSizeDp)
        }
    }
}

@Composable
fun getSVGImageModel(url: String) =
    ImageRequest
        .Builder(LocalContext.current)
        .data(url)
        .decoderFactory(SvgDecoder.Factory())
        .build()

const val DEFAULT_LOADER_SIZE = 20
const val DEFAULT_LOADER_PERCENTAGE = 0.25f

sealed class LoaderSize(
    val mSize: Dp,
) {
    data object None : LoaderSize(mSize = 0.dp)
    data object Fixed : LoaderSize(mSize = DEFAULT_LOADER_SIZE.dp)
    data class Dynamic(
        val size: Dp,
    ) : LoaderSize(mSize = size)
    data class Percentage(
        val percentage: Float = DEFAULT_LOADER_PERCENTAGE,
    ) : LoaderSize(mSize = Dp.Unspecified)
}
