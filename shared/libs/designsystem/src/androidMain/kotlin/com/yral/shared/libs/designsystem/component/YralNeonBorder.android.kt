package com.yral.shared.libs.designsystem.component

import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

internal actual fun Modifier.neonBorder(
    glowingColor: Color,
    containerColor: Color,
    glowingRadius: Dp,
    cornerRadius: Dp,
    xShifting: Dp,
    yShifting: Dp,
) = this.drawBehind {
    val canvasSize = size
    drawContext.canvas.nativeCanvas.apply {
        drawRoundRect(
            0f, // Left
            0f, // Top
            canvasSize.width, // Right
            canvasSize.height, // Bottom
            cornerRadius.toPx(), // Radius X
            cornerRadius.toPx(), // Radius Y
            Paint().apply {
                color = containerColor.toArgb()
                isAntiAlias = true
                setShadowLayer(
                    glowingRadius.toPx(),
                    xShifting.toPx(),
                    yShifting.toPx(),
                    glowingColor.toArgb(),
                )
            },
        )
    }
}
