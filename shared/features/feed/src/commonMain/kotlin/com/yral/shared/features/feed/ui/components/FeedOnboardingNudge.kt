package com.yral.shared.features.feed.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralBrushes
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.feed.generated.resources.Res
import yral_mobile.shared.features.feed.generated.resources.next
import yral_mobile.shared.libs.designsystem.generated.resources.onboarding_arrow_top
import yral_mobile.shared.libs.designsystem.generated.resources.onboarding_nudge_stars
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

data class FeedTargetBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Simple onboarding nudge for feed top elements (DailyRank and CoinBalance).
 * @param targetBounds The bounds of the target element to create a transparent hole in the scrim (optional)
 */
@Composable
fun FeedOnboardingNudge(
    text: String,
    highlightText: String? = null,
    arrowAlignment: ArrowAlignment,
    targetBounds: FeedTargetBounds? = null,
    isDismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isShowNext: Boolean = true,
) {
    val (offsetY, alpha) = rememberNudgeAnimation()

    // Capture overlay position to adjust target bounds from root coordinates to overlay-relative
    var overlayPosition by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    overlayPosition = Offset(position.x, position.y)
                },
    ) {
        // Scrim with transparent hole at target element
        ScrimWithCutout(
            targetBounds = targetBounds,
            overlayPosition = overlayPosition,
            isDismissible = isDismissible,
            onDismiss = onDismiss,
            {
                val arrowTopPadding = 72.dp // Below target element
                ArrowIndicator(
                    alignment = arrowAlignment,
                    topPadding = arrowTopPadding,
                    offsetY = offsetY,
                    alpha = alpha,
                )
                // Text and stars - centered on screen
                OnboardingTextContent(
                    text = text,
                    highlightText = highlightText,
                    topPadding = arrowTopPadding + 157.dp + 16.dp, // Below arrow
                    offsetY = offsetY,
                    alpha = alpha,
                    onDismiss = onDismiss,
                    isShowNext = isShowNext,
                )
            },
        )
    }
}

@Composable
private fun ScrimWithCutout(
    targetBounds: FeedTargetBounds?,
    overlayPosition: Offset?,
    isDismissible: Boolean,
    onDismiss: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    // Draw scrim with semi-transparent cutout area
                    if (targetBounds != null && overlayPosition != null) {
                        val padding = with(density) { 8.dp.toPx() } // Padding around the target element
                        val cornerRadius = with(density) { 32.dp.toPx() }

                        // Convert root-relative bounds to overlay-relative bounds
                        val adjustedLeft = targetBounds.left - overlayPosition.x
                        val adjustedTop = targetBounds.top - overlayPosition.y
                        val adjustedRight = targetBounds.right - overlayPosition.x
                        val adjustedBottom = targetBounds.bottom - overlayPosition.y

                        // Create paths for scrim and cutout
                        val scrimPath =
                            Path().apply {
                                addRect(Rect(0f, 0f, size.width, size.height))
                            }
                        val cutoutPath =
                            Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = adjustedLeft - padding,
                                        top = adjustedTop - padding,
                                        right = adjustedRight + padding,
                                        bottom = adjustedBottom + padding,
                                        radiusX = cornerRadius,
                                        radiusY = cornerRadius,
                                    ),
                                )
                            }

                        // Draw full scrim everywhere EXCEPT the cutout area
                        val scrimWithoutCutout = Path.combine(PathOperation.Difference, scrimPath, cutoutPath)
                        drawPath(scrimWithoutCutout, color = YralColors.ScrimColorLight)

                        // Draw semi-transparent scrim (0.5f alpha) ONLY in the cutout area
                        drawPath(cutoutPath, color = YralColors.ScrimColorLight.copy(alpha = 0.3f))
                    } else {
                        // Fallback: full scrim if no bounds
                        drawRect(color = YralColors.ScrimColorLight)
                    }
                    // Draw the content (arrow and text) on top of the scrim
                    drawContent()
                }.then(
                    if (isDismissible && onDismiss != null) {
                        Modifier.clickable { onDismiss() }
                    } else {
                        Modifier
                    },
                ),
    ) {
        content()
    }
}

@Composable
private fun rememberNudgeAnimation(durationMillis: Int = 600): Pair<Float, Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "feed_onboarding_nudge")
    val tweenSpec =
        tween<Float>(
            durationMillis = durationMillis,
            easing = FastOutLinearInEasing,
        )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
        label = "offsetY",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
        label = "alpha",
    )
    return offsetY to alpha
}

@Composable
private fun BoxScope.ArrowIndicator(
    alignment: ArrowAlignment,
    topPadding: Dp,
    offsetY: Float,
    alpha: Float,
) {
    when (alignment) {
        ArrowAlignment.TOP_START -> {
            Box(
                modifier =
                    Modifier
                        .padding(top = topPadding + offsetY.dp, start = 26.dp)
                        .align(Alignment.TopStart),
            ) {
                Image(
                    painter = painterResource(DesignRes.drawable.onboarding_arrow_top),
                    contentDescription = "arrow",
                    modifier =
                        Modifier
                            .alpha(alpha)
                            .graphicsLayer {
                                scaleX = -1f // Mirror for left side
                            },
                    contentScale = ContentScale.Fit,
                )
            }
        }
        ArrowAlignment.TOP_END -> {
            Box(
                modifier =
                    Modifier
                        .padding(top = topPadding + offsetY.dp, end = 26.dp)
                        .align(Alignment.TopEnd),
            ) {
                Image(
                    painter = painterResource(DesignRes.drawable.onboarding_arrow_top),
                    contentDescription = "arrow",
                    modifier = Modifier.alpha(alpha),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.OnboardingTextContent(
    text: String,
    highlightText: String?,
    topPadding: Dp,
    offsetY: Float,
    alpha: Float,
    onDismiss: (() -> Unit)?,
    isShowNext: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = topPadding + offsetY.dp)
                .align(Alignment.TopCenter),
    ) {
        val density = LocalDensity.current
        var textWidth by remember { mutableIntStateOf(0) }

        // Stars background
        StarsBackground(
            textWidth = textWidth,
            alpha = alpha,
            density = density,
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text =
                    buildAnnotatedTextWithHighlight(
                        text = text,
                        highlightText = highlightText,
                    ),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .alpha(alpha)
                        .padding(horizontal = 48.dp)
                        .onGloballyPositioned { textWidth = it.size.width },
            )
            if (isShowNext) {
                YralButton(
                    modifier = Modifier.wrapContentSize().widthIn(min = 120.dp),
                    text = stringResource(Res.string.next),
                    borderColor = YralColors.Neutral700,
                    borderWidth = 1.dp,
                    backgroundColor = YralColors.Neutral800,
                    textStyle = TextStyle(color = YralColors.NeutralTextPrimary),
                    onClick = onDismiss ?: {},
                )
            }
        }
    }
}

@Composable
private fun BoxScope.StarsBackground(
    textWidth: Int,
    alpha: Float,
    density: Density,
) {
    Image(
        painter = painterResource(DesignRes.drawable.onboarding_nudge_stars),
        contentDescription = null,
        modifier =
            Modifier
                .width(with(density) { maxOf(0.dp, textWidth.toDp() - 16.dp) })
                .height(130.dp)
                .alpha(alpha)
                .align(Alignment.Center),
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
private fun buildAnnotatedTextWithHighlight(
    text: String,
    highlightText: String?,
): AnnotatedString {
    val textStyle = LocalAppTopography.current.xlBold
    val spanStyle =
        SpanStyle(
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
            color = YralColors.Neutral50,
        )
    return buildAnnotatedString {
        if (highlightText != null && text.contains(highlightText)) {
            val highlightStart = text.indexOf(highlightText)
            val highlightEnd = highlightStart + highlightText.length
            if (highlightStart > 0) {
                withStyle(style = spanStyle) { append(text.take(highlightStart)) }
            }
            withStyle(style = spanStyle.copy(brush = YralBrushes.GoldenTextBrush)) {
                append(text.substring(highlightStart, highlightEnd))
            }

            if (highlightEnd < text.length) {
                withStyle(style = spanStyle) { append(text.substring(highlightEnd)) }
            }
        } else {
            withStyle(style = spanStyle) { append(text) }
        }
    }
}

/**
 * Modifier that captures the target element's bounds for the onboarding nudge cutout.
 * Use this modifier on the target element that should be highlighted.
 * @param targetBounds Callback to receive the captured bounds
 * @param adjustBounds Optional function to adjust the captured bounds (e.g., to account for internal offsets)
 */
@Composable
fun Modifier.captureFeedOnboardingBounds(
    targetBounds: (FeedTargetBounds?) -> Unit,
    adjustBounds: ((FeedTargetBounds) -> FeedTargetBounds)? = null,
): Modifier =
    this.then(
        Modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size
            var bounds =
                FeedTargetBounds(
                    left = position.x,
                    top = position.y,
                    right = position.x + size.width,
                    bottom = position.y + size.height,
                )
            // Apply adjustment if provided (e.g., to account for bag offset in CoinBalance)
            if (adjustBounds != null) {
                bounds = adjustBounds(bounds)
            }
            targetBounds(bounds)
        },
    )

enum class ArrowAlignment {
    TOP_START,
    TOP_END,
}
