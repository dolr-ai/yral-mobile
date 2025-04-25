package com.yral.android.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors

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
                .fillMaxWidth()
                .height(45.dp)
                .paint(
                    painter = painterResource(getButtonBackground(buttonType, buttonState)),
                    contentScale = ContentScale.FillBounds,
                ).clickable {
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

private fun getButtonBackground(
    buttonType: YralButtonType,
    buttonState: YralButtonState,
): Int =
    when (buttonType) {
        YralButtonType.Pink ->
            when (buttonState) {
                YralButtonState.Enabled -> R.drawable.pink_gradient
                YralButtonState.Disabled -> R.drawable.disbaled_pink_gradient
                YralButtonState.Loading -> R.drawable.pink_gradient
            }

        YralButtonType.White ->
            when (buttonState) {
                YralButtonState.Enabled -> R.drawable.white_background
                YralButtonState.Disabled -> R.drawable.white_background_disabled
                YralButtonState.Loading -> R.drawable.white_background
            }
    }
