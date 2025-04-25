package com.yral.android.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.pinkDisabledGradient
import com.yral.android.ui.design.pinkGradient

@Composable
fun YralButton(
    buttonState: YralButtonState = YralButtonState.Enabled,
    buttonType: YralButtonType = YralButtonType.Pink,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .padding(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp,
                    bottom = 12.dp,
                ).fillMaxWidth()
                .height(45.dp)
                .drawWithCache {
                    val gradient =
                        getBackGroundBrush(
                            buttonType = buttonType,
                            buttonState = buttonState,
                            size = size,
                        )
                    onDrawBehind {
                        drawRoundRect(
                            brush = gradient,
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )
                    }
                }.clickable {
                    if (buttonState == YralButtonState.Enabled) {
                        onClick()
                    }
                },
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.mdBold,
            color = getTextColor(buttonType, buttonState),
        )
    }
}

enum class YralButtonState {
    Enabled,
    Disabled,
    Loading,
}

enum class YralButtonType {
    Pink,
    White,
}

private fun getTextColor(
    buttonType: YralButtonType,
    buttonState: YralButtonState,
): Color =
    when (buttonType) {
        YralButtonType.Pink ->
            when (buttonState) {
                YralButtonState.Enabled -> YralColors.Neutral50
                YralButtonState.Disabled -> YralColors.Pink100
                YralButtonState.Loading -> YralColors.Neutral50
            }

        YralButtonType.White ->
            when (buttonState) {
                YralButtonState.Enabled -> YralColors.Pink300
                YralButtonState.Disabled -> YralColors.Pink300
                YralButtonState.Loading -> YralColors.Pink300
            }
    }

private fun getBackGroundBrush(
    buttonType: YralButtonType,
    buttonState: YralButtonState,
    size: Size,
): Brush =
    when (buttonType) {
        YralButtonType.Pink ->
            when (buttonState) {
                YralButtonState.Enabled -> pinkGradient(size)
                YralButtonState.Disabled -> pinkDisabledGradient(size)
                YralButtonState.Loading -> pinkGradient(size)
            }

        YralButtonType.White ->
            when (buttonState) {
                YralButtonState.Enabled -> pinkGradient(size)
                YralButtonState.Disabled -> pinkGradient(size)
                YralButtonState.Loading -> pinkGradient(size)
            }
    }
