package com.yral.shared.features.tournament.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object CircularTimerConstants {
    const val TIMER_SIZE = 63
    const val STROKE_WIDTH = 4f
    const val ARC_SEGMENTS = 60 // Number of segments for smooth gradient
}

@Suppress("MagicNumber")
private val timerBackgroundColor = Color(0xFF1A1A1A)

@Suppress("MagicNumber")
private val timerGradientColors =
    listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Red
    )

/**
 * A circular progress timer that displays time remaining with an animated gradient arc.
 *
 * The progress arc fills clockwise from the top as the tournament progresses.
 * The gradient is distributed across the visible arc length (not the full circle).
 * The timer text displays MM:SS with vertical slide animation when values change.
 *
 * @param timeLeftMs Time remaining in milliseconds
 * @param totalDurationMs Total duration of the tournament in milliseconds
 * @param modifier Optional modifier for the composable
 */
@Suppress("MagicNumber")
@Composable
fun CircularTimerProgress(
    timeLeftMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier,
) {
    val progress = calculateProgress(timeLeftMs, totalDurationMs)
    val (minutes, seconds) = formatTimeComponents(timeLeftMs)

    Box(
        modifier = modifier.size(CircularTimerConstants.TIMER_SIZE.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressCanvas(progress = progress, modifier = Modifier.fillMaxSize())
        TimerText(minutes = minutes, seconds = seconds)
    }
}

@Suppress("MagicNumber")
private fun calculateProgress(
    timeLeftMs: Long,
    totalDurationMs: Long,
): Float =
    if (totalDurationMs > 0) {
        // Start with full circle, decrease as time passes
        (timeLeftMs.toFloat() / totalDurationMs.toFloat())
    } else {
        0f
    }.coerceIn(0f, 1f)

@Suppress("MagicNumber")
private fun formatTimeComponents(timeLeftMs: Long): Pair<String, String> {
    val totalSeconds = (timeLeftMs / 1000).coerceAtLeast(0)
    val minutes = (totalSeconds / 60).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return minutes to seconds
}

@Composable
private fun CircularProgressCanvas(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = CircularTimerConstants.STROKE_WIDTH.dp.toPx()
        drawBackgroundCircle()
        if (progress > 0f) {
            drawProgressArcWithDistributedGradient(progress, strokeWidth)
        }
    }
}

@Suppress("MagicNumber")
private fun DrawScope.drawBackgroundCircle() {
    val canvasSize = size.minDimension
    val center = Offset(size.width / 2, size.height / 2)
    drawCircle(
        color = timerBackgroundColor,
        radius = canvasSize / 2,
        center = center,
    )
}

/**
 * Draws the progress arc with the gradient distributed across only the visible portion.
 * This means at any progress level, the full gradient (green → yellow → orange → red)
 * is visible and compressed into the current arc length.
 */
@Suppress("MagicNumber")
private fun DrawScope.drawProgressArcWithDistributedGradient(
    progress: Float,
    strokeWidth: Float,
) {
    val canvasSize = size.minDimension
    val radius = (canvasSize - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    val totalSweepAngle = progress * 360f
    val startAngle = -90f // Start from top

    // Draw arc in segments for smooth gradient distribution
    val segments = CircularTimerConstants.ARC_SEGMENTS
    val segmentAngle = totalSweepAngle / segments

    for (i in 0 until segments) {
        // Calculate the gradient position (0.0 to 1.0) for this segment
        val gradientPosition = i.toFloat() / (segments - 1).coerceAtLeast(1)
        val segmentColor = interpolateGradientColor(gradientPosition)

        val segmentStartAngle = startAngle + (i * segmentAngle)

        // Add small overlap to prevent gaps between segments
        val adjustedSegmentAngle = segmentAngle + 0.5f

        drawArc(
            color = segmentColor,
            startAngle = segmentStartAngle,
            sweepAngle = adjustedSegmentAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
        )
    }

    // Draw rounded caps at start and end
    drawArc(
        color = timerGradientColors.first(),
        startAngle = startAngle,
        sweepAngle = 0.1f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    drawArc(
        color = timerGradientColors.last(),
        startAngle = startAngle + totalSweepAngle - 0.1f,
        sweepAngle = 0.1f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

/**
 * Interpolates a color from the gradient based on position (0.0 to 1.0).
 */
@Suppress("MagicNumber")
private fun interpolateGradientColor(position: Float): Color {
    val colors = timerGradientColors
    if (colors.size < 2) return colors.firstOrNull() ?: Color.White

    val scaledPosition = position.coerceIn(0f, 1f) * (colors.size - 1)
    val lowerIndex = scaledPosition.toInt().coerceIn(0, colors.size - 2)
    val upperIndex = (lowerIndex + 1).coerceAtMost(colors.size - 1)
    val fraction = scaledPosition - lowerIndex

    return lerpColor(colors[lowerIndex], colors[upperIndex], fraction)
}

/**
 * Linear interpolation between two colors.
 */
@Suppress("MagicNumber")
private fun lerpColor(
    start: Color,
    end: Color,
    fraction: Float,
): Color =
    Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction,
    )

@Composable
private fun TimerText(
    minutes: String,
    seconds: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedTimerDigit(value = minutes)
        Text(
            text = ":",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        AnimatedTimerDigit(value = seconds)
    }
}

/**
 * A single timer digit (or pair of digits) with vertical slide animation.
 */
@Composable
private fun AnimatedTimerDigit(value: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()) togetherWith
                (slideOutVertically { -it } + fadeOut())
        },
        label = "TimerDigit",
    ) { digit ->
        Text(
            text = digit,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}
