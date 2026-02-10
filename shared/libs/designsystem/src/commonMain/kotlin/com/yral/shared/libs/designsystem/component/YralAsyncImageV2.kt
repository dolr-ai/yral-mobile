package com.yral.shared.libs.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Performance-optimized version of YralAsyncImage.
 *
 * Key improvements over V1:
 * - Removed BoxWithConstraints (expensive measurements)
 * - Optional loader for performance-critical scenarios (grids, lists)
 * - Uses graphicsLayer for better composition performance
 * - More efficient state management
 * - Configurable crossfade animation
 *
 * @param imageUrl The image URL or ImageRequest
 * @param modifier Modifier for the container
 * @param showLoader Whether to show loading indicator (disable for scrolling lists)
 * @param loaderSize Size configuration for the loader
 * @param enableCrossfade Enable crossfade animation (disable for better scroll performance)
 * @param border Border width
 * @param borderColor Border color
 * @param backgroundColor Background color shown while loading
 * @param contentScale How to scale the image
 * @param shape Shape for clipping and border
 * @param onError Callback when image fails to load
 */
@Composable
fun YralAsyncImageV2(
    imageUrl: Any,
    modifier: Modifier = Modifier,
    showLoader: Boolean = true,
    loaderSize: LoaderSize = LoaderSize.Percentage(),
    enableCrossfade: Boolean = true,
    border: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    backgroundColor: Color = Color.Transparent,
    contentScale: ContentScale = ContentScale.FillBounds,
    shape: Shape = CircleShape,
    onError: () -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val imageRequest = rememberImageRequest(imageUrl, enableCrossfade, context)
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier =
            modifier
                .graphicsLayer() // Hardware acceleration for better performance
                .clip(shape)
                .then(
                    if (border > 0.dp) {
                        Modifier.border(width = border, color = borderColor, shape = shape)
                    } else {
                        Modifier
                    },
                ).background(color = backgroundColor, shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = "image",
            contentScale = contentScale,
            modifier = Modifier.matchParentSize(),
            onState = { state ->
                isLoading = state !is AsyncImagePainter.State.Success
                if (state is AsyncImagePainter.State.Error) {
                    onError()
                }
            },
        )

        if (showLoader) {
            LoaderOverlay(isLoading = isLoading, loaderSize = loaderSize)
        }
    }
}

@Composable
private fun rememberImageRequest(
    imageUrl: Any,
    enableCrossfade: Boolean,
    context: coil3.PlatformContext,
): ImageRequest =
    remember(imageUrl, enableCrossfade) {
        when (imageUrl) {
            is ImageRequest -> imageUrl
            else ->
                ImageRequest
                    .Builder(context)
                    .data(imageUrl)
                    .crossfade(enableCrossfade)
                    .build()
        }
    }

@Composable
private fun LoaderOverlay(
    isLoading: Boolean,
    loaderSize: LoaderSize,
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val loaderSizeDp =
            when (loaderSize) {
                is LoaderSize.None -> 0.dp
                is LoaderSize.Fixed -> loaderSize.mSize
                is LoaderSize.Dynamic -> loaderSize.size
                is LoaderSize.Percentage -> DEFAULT_LOADER_SIZE.dp
            }
        if (loaderSizeDp > 0.dp) {
            YralLoader(size = loaderSizeDp)
        }
    }
}

/**
 * Shimmer variant of YralAsyncImageV2 with performance optimizations.
 */
@Composable
fun YralShimmerImageV2(
    imageUrl: Any?,
    placeholderImageUrl: Any,
    modifier: Modifier = Modifier,
    enableCrossfade: Boolean = true,
    border: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    backgroundColor: Color = Color.Transparent,
    contentScale: ContentScale = ContentScale.FillBounds,
    shape: Shape = CircleShape,
    onError: () -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val targetUrl = imageUrl ?: placeholderImageUrl
    val imageRequest = rememberImageRequest(targetUrl, enableCrossfade, context)
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier =
            modifier
                .graphicsLayer()
                .clip(shape)
                .then(
                    if (border > 0.dp) {
                        Modifier.border(width = border, color = borderColor, shape = shape)
                    } else {
                        Modifier
                    },
                ).background(color = backgroundColor, shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = "image",
            contentScale = contentScale,
            modifier = Modifier.matchParentSize(),
            onState = { state ->
                isLoading = state !is AsyncImagePainter.State.Success
                if (state is AsyncImagePainter.State.Error) {
                    onError()
                }
            },
        )

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .shimmer(cornerRadius = 4.dp),
            )
        }
    }
}

/**
 * Grid-optimized variant specifically for scrolling lists.
 * Disables loader and crossfade for maximum performance.
 */
@Composable
fun YralGridImage(
    imageUrl: Any,
    modifier: Modifier = Modifier,
    border: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    backgroundColor: Color = Color.Transparent,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = CircleShape,
    onError: () -> Unit = {},
) {
    YralAsyncImageV2(
        imageUrl = imageUrl,
        modifier = modifier,
        showLoader = false,
        enableCrossfade = false,
        border = border,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        contentScale = contentScale,
        shape = shape,
        onError = onError,
    )
}
