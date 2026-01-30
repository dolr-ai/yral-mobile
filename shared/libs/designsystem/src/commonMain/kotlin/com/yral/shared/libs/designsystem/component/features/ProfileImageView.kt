package com.yral.shared.libs.designsystem.component.features

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import kotlin.math.round
import kotlin.math.sqrt

@Composable
fun ProfileImageView(
    imageUrl: String,
    size: Dp = 76.dp,
    shape: Shape = CircleShape,
    applyFrame: Boolean = false,
    frameBorderWidth: Dp = 3.dp,
    frameBrush: Brush = proBrush(),
    frameBadgeIcon: DrawableResource = Res.drawable.ic_thunder,
    frameBadgeSizeFraction: Float = 0.22f,
    frameBadgeOverflowFraction: Float = 0.2f,
) {
    val frameBadgeSize = (size.value * frameBadgeSizeFraction).dp
    val frameBadgeOverflow =
        if (applyFrame) {
            (frameBadgeSize.value * frameBadgeOverflowFraction).dp
        } else {
            0.dp
        }
    Box(modifier = Modifier.profileImageOuterPadding(applyFrame, frameBadgeOverflow)) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .graphicsLayer { clip = false },
        ) {
            ProfileImageContent(
                imageUrl = imageUrl,
                size = size,
                shape = shape,
                applyFrame = applyFrame,
                frameBorderWidth = frameBorderWidth,
                frameBrush = frameBrush,
                frameBadgeSize = frameBadgeSize,
                frameBadgeIcon = frameBadgeIcon,
            )
        }
    }
}

fun proBrush() = linearGradient(colors = listOf(YralColors.Yellow200, YralColors.Yellow300))

private fun cornerRadiusPxFromShape(
    shape: Shape,
    sizePx: Float,
    density: Density,
): Float =
    (shape as? CornerBasedShape)
        ?.bottomEnd
        ?.toPx(Size(sizePx, sizePx), density)
        ?: 0f

@Suppress("MagicNumber")
private fun computeProBadgeOffsetPx(
    shape: Shape,
    size: Dp,
    frameBadgeSize: Dp,
    density: Density,
): Pair<Float, Float> =
    with(density) {
        val sizePx = size.toPx()
        val frameBadgePx = frameBadgeSize.toPx()
        val sqrt2 = sqrt(2.0).toFloat()
        when {
            shape === CircleShape -> {
                val o =
                    (frameBadgePx / 2 - sizePx / 2 + sizePx / (2 * sqrt(2.0))).toFloat()
                o to o
            }
            shape === RectangleShape -> {
                val nudgePx = (frameBadgeSize.value * 0.4f).dp.toPx()
                nudgePx to -nudgePx
            }
            else -> {
                val cornerPx = cornerRadiusPxFromShape(shape, sizePx, density)
                val o = frameBadgePx / 2 - cornerPx * (1f - 1f / sqrt2)
                o to o
            }
        }
    }

private fun Modifier.profileImageOuterPadding(
    applyFrame: Boolean,
    frameBadgeOverflow: Dp,
): Modifier =
    then(
        if (applyFrame) {
            Modifier.padding(
                end = frameBadgeOverflow,
                bottom = frameBadgeOverflow,
            )
        } else {
            Modifier
        },
    )

private fun Modifier.profileImageBorder(
    applyFrame: Boolean,
    frameBorderWidth: Dp,
    frameBrush: Brush,
    shape: Shape,
): Modifier =
    then(
        if (applyFrame) {
            Modifier.border(
                width = frameBorderWidth,
                brush = frameBrush,
                shape = shape,
            )
        } else {
            Modifier
        },
    )

@Composable
private fun BoxScope.ProBadgeImage(
    frameBadgeSize: Dp,
    size: Dp,
    shape: Shape,
    frameBadgeIcon: DrawableResource,
) {
    Image(
        painter = painterResource(frameBadgeIcon),
        contentDescription = "Pro badge",
        contentScale = ContentScale.Inside,
        modifier =
            Modifier
                .size(frameBadgeSize)
                .align(Alignment.BottomEnd)
                .offset {
                    val (offsetXPx, offsetYPx) =
                        computeProBadgeOffsetPx(shape, size, frameBadgeSize, this)
                    IntOffset(
                        round(offsetXPx.toDouble()).toInt(),
                        round(offsetYPx.toDouble()).toInt(),
                    )
                },
    )
}

@Composable
private fun BoxScope.ProfileImageContent(
    imageUrl: String,
    size: Dp,
    shape: Shape,
    applyFrame: Boolean,
    frameBorderWidth: Dp,
    frameBrush: Brush,
    frameBadgeSize: Dp,
    frameBadgeIcon: DrawableResource,
) {
    YralAsyncImage(
        imageUrl = imageUrl,
        shape = shape,
        modifier =
            Modifier
                .size(size)
                .profileImageBorder(
                    applyFrame,
                    frameBorderWidth,
                    frameBrush,
                    shape,
                ),
        contentScale = ContentScale.Crop,
    )
    if (applyFrame) {
        ProBadgeImage(
            frameBadgeSize = frameBadgeSize,
            size = size,
            shape = shape,
            frameBadgeIcon = frameBadgeIcon,
        )
    }
}

private data class ProfileImageViewPreviewParameter(
    val shape: Shape,
    val size: Dp,
)

private class ProfileImageViewPreviewParameterProvider : PreviewParameterProvider<ProfileImageViewPreviewParameter> {
    private val roundedCornerRadius = 24.dp
    private val shapeParams =
        listOf(
            ProfileImageViewPreviewParameter(CircleShape, 40.dp),
            ProfileImageViewPreviewParameter(CircleShape, 56.dp),
            ProfileImageViewPreviewParameter(CircleShape, 76.dp),
            ProfileImageViewPreviewParameter(CircleShape, 96.dp),
            ProfileImageViewPreviewParameter(RectangleShape, 40.dp),
            ProfileImageViewPreviewParameter(RectangleShape, 56.dp),
            ProfileImageViewPreviewParameter(RectangleShape, 76.dp),
            ProfileImageViewPreviewParameter(RectangleShape, 96.dp),
            ProfileImageViewPreviewParameter(
                RoundedCornerShape(roundedCornerRadius),
                40.dp,
            ),
            ProfileImageViewPreviewParameter(
                RoundedCornerShape(roundedCornerRadius),
                56.dp,
            ),
            ProfileImageViewPreviewParameter(
                RoundedCornerShape(roundedCornerRadius),
                76.dp,
            ),
            ProfileImageViewPreviewParameter(
                RoundedCornerShape(roundedCornerRadius),
                96.dp,
            ),
        )

    override val values: Sequence<ProfileImageViewPreviewParameter> = shapeParams.asSequence()
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun ProfileImageViewPreview(
    @PreviewParameter(ProfileImageViewPreviewParameterProvider::class)
    parameter: ProfileImageViewPreviewParameter,
) {
    ProfileImageView(
        imageUrl = "https://picsum.photos/200",
        size = parameter.size,
        shape = parameter.shape,
        applyFrame = true,
        frameBorderWidth = 2.dp,
    )
}
