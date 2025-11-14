package com.yral.shared.libs.designsystem.theme

import androidx.compose.ui.graphics.Color
import platform.UIKit.UIColor

fun Color.toUiColor(): UIColor =
    UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
