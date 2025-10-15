package com.yral.shared.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.libs.designsystem.theme.YralColors

@Suppress("MagicNumber")
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            darkColorScheme(
                primary = YralColors.Pink300,
                secondary = Color(0xFF03DAC5),
                tertiary = Color(0xFF3700B3),
                primaryContainer = YralColors.PrimaryContainer,
                onPrimaryContainer = YralColors.OnPrimaryContainer,
            )
        } else {
            lightColorScheme(
                primary = YralColors.Pink300,
                secondary = Color(0xFF03DAC5),
                tertiary = Color(0xFF3700B3),
                primaryContainer = YralColors.PrimaryContainer,
                onPrimaryContainer = YralColors.OnPrimaryContainer,
            )
        }
    val typography =
        Typography(
            bodyMedium =
                TextStyle(
                    fontFamily = FontFamily.Companion.Default,
                    fontWeight = FontWeight.Companion.Normal,
                    fontSize = 16.sp,
                ),
        )
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(4.dp),
            large =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(0.dp),
        )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
