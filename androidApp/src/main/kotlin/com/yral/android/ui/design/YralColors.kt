@file:Suppress("MagicNumber")

package com.yral.android.ui.design

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object YralColors {
    val PrimaryContainer: Color = Color(0xFF0A0A0A)
    val OnPrimaryContainer: Color = Color(0xFFFAFAFA)

    val NeutralTextPrimary: Color = Color(0xFFFAFAFA)
    val NeutralTextSecondary: Color = Color(0xFFA3A3A3)

    val Neutral50: Color = Color(0xFFFAFAFA)
    val Neutral500: Color = Color(0xFFA3A3A3)
    val Neutral900: Color = Color(0xFF171717)

    val Pink300: Color = Color(0xFFE2017B)

    val Divider: Color = Color(0xFF232323)
}

fun pinkGradient(size: Size): Brush {
    val angle = Math.toRadians(128.273)
    val radius = hypot(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    val dx = cos(angle).toFloat() * radius
    val dy = sin(angle).toFloat() * radius

    val start = Offset(center.x - dx, center.y - dy)
    val end = Offset(center.x + dx, center.y + dy)

    return Brush.linearGradient(
        colorStops =
            arrayOf(
                0.0f to Color(0xFFFF78C1),
                0.509f to Color(0xFFE2017B),
                1.0f to Color(0xFFAD005E),
            ),
        start = start,
        end = end,
    )
}
