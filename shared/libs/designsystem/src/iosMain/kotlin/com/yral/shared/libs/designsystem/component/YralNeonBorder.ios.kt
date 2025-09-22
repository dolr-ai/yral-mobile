package com.yral.shared.libs.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

internal actual fun Modifier.neonBorder(
    glowingColor: Color,
    containerColor: Color,
    glowingRadius: Dp,
    cornerRadius: Dp,
    xShifting: Dp,
    yShifting: Dp,
): Modifier = this
