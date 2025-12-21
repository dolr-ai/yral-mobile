package com.yral.shared.libs.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class GradientAngleConvention {
    // 0° = left -> right, 90° = top -> bottom
    ComposeDegrees,

    // 0° = up, 90° = right (CSS)
    CssDegrees,
}

enum class GradientLengthMode {
    // Gradient line is clamped to the rectangle edges.
    EdgeClamped,

    // Gradient line uses half-diagonal length (CSS-like).
    Diagonal,
}

@Suppress("MagicNumber")
fun linearGradientBrush(
    colorStops: Array<Pair<Float, Color>>,
    angleDegrees: Float,
    size: Size,
    angleConvention: GradientAngleConvention = GradientAngleConvention.ComposeDegrees,
    lengthMode: GradientLengthMode = GradientLengthMode.EdgeClamped,
): Brush? {
    val composeDegrees =
        when (angleConvention) {
            GradientAngleConvention.ComposeDegrees -> angleDegrees
            GradientAngleConvention.CssDegrees -> (angleDegrees - 90f)
        }

    val alpha = ((composeDegrees % 360).let { if (it < 0) it + 360 else it } * PI / 180).toFloat()
    val center = Offset(size.width / 2f, size.height / 2f)

    return when (lengthMode) {
        GradientLengthMode.EdgeClamped -> {
            val (x, y) = size
            val gamma = atan2(y, x)
            if (gamma == 0f || gamma == (PI / 2).toFloat()) {
                // degenerate rectangle
                null
            } else {
                val gradientLength =
                    when (alpha) {
                        // ray from centre cuts the right edge of the rectangle
                        in 0f..gamma, in (2 * PI - gamma)..2 * PI -> {
                            x / cos(alpha)
                        }
                        // ray from centre cuts the top edge of the rectangle
                        in gamma..(PI - gamma).toFloat() -> {
                            y / sin(alpha)
                        }
                        // ray from centre cuts the left edge of the rectangle
                        in (PI - gamma)..(PI + gamma) -> {
                            x / -cos(alpha)
                        }
                        // ray from centre cuts the bottom edge of the rectangle
                        in (PI + gamma)..(2 * PI - gamma) -> {
                            y / -sin(alpha)
                        }
                        // default case (which shouldn't really happen)
                        else -> hypot(x, y)
                    }

                val centerOffsetX = cos(alpha) * gradientLength / 2
                val centerOffsetY = sin(alpha) * gradientLength / 2

                Brush.linearGradient(
                    colorStops = colorStops,
                    // negative here so that 0 degrees is left -> right
                    // and 90 degrees is top -> bottom
                    start = Offset(size.center.x - centerOffsetX, size.center.y - centerOffsetY),
                    end = Offset(size.center.x + centerOffsetX, size.center.y + centerOffsetY),
                )
            }
        }
        GradientLengthMode.Diagonal -> {
            val dx = cos(alpha)
            val dy = sin(alpha)
            val halfDiagonal = hypot(size.width, size.height) / 2f
            val start = center - Offset(dx, dy) * halfDiagonal
            val end = center + Offset(dx, dy) * halfDiagonal
            Brush.linearGradient(colorStops = colorStops, start = start, end = end)
        }
    }
}

@Suppress("MagicNumber")
fun Modifier.angledGradientBackground(
    colorStops: Array<Pair<Float, Color>>,
    degrees: Float,
    angleConvention: GradientAngleConvention = GradientAngleConvention.ComposeDegrees,
    lengthMode: GradientLengthMode = GradientLengthMode.EdgeClamped,
) = this.then(
    drawBehind {
        val brush =
            linearGradientBrush(
                colorStops = colorStops,
                angleDegrees = degrees,
                size = size,
                angleConvention = angleConvention,
                lengthMode = lengthMode,
            )
                ?: return@drawBehind

        drawRect(
            brush = brush,
            size = size,
        )
    },
)
