package com.yral.android.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieConstants
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography

@Composable
fun YralGradientButton(
    modifier: Modifier = Modifier,
    buttonState: YralButtonState = YralButtonState.Enabled,
    buttonType: YralButtonType = YralButtonType.Pink,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
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
        if (buttonState != YralButtonState.Loading) {
            YralMaskedVectorText(
                text = text,
                vectorRes = getButtonTextBackground(buttonType, buttonState),
                textStyle =
                    LocalAppTopography
                        .current
                        .mdBold
                        .plus(
                            TextStyle(
                                textAlign = TextAlign.Center,
                            ),
                        ),
            )
        } else {
            // The continuous loader animation
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                YralLottieAnimation(
                    modifier = Modifier.size(20.dp),
                    rawRes = getLoaderResource(buttonType),
                    iterations = LottieConstants.IterateForever,
                )
            }
        }
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

private fun getLoaderResource(buttonType: YralButtonType): Int =
    when (buttonType) {
        YralButtonType.Pink -> R.raw.white_loader
        YralButtonType.White -> R.raw.pink_loader
    }

private fun getButtonTextBackground(
    buttonType: YralButtonType,
    buttonState: YralButtonState,
): Int =
    when (buttonType) {
        YralButtonType.Pink ->
            when (buttonState) {
                YralButtonState.Enabled -> R.drawable.white_background
                YralButtonState.Disabled -> R.drawable.white_background_disabled
                YralButtonState.Loading -> R.drawable.white_background
            }

        YralButtonType.White ->
            when (buttonState) {
                YralButtonState.Enabled -> R.drawable.pink_gradient_background
                YralButtonState.Disabled -> R.drawable.pink_gradient_background_disabled
                YralButtonState.Loading -> R.drawable.pink_gradient_background
            }
    }

private fun getButtonBackground(
    buttonType: YralButtonType,
    buttonState: YralButtonState,
): Int =
    when (buttonType) {
        YralButtonType.Pink ->
            when (buttonState) {
                YralButtonState.Enabled -> R.drawable.pink_gradient_background
                YralButtonState.Disabled -> R.drawable.pink_gradient_background_disabled
                YralButtonState.Loading -> R.drawable.pink_gradient_background
            }

        YralButtonType.White ->
            when (buttonState) {
                YralButtonState.Enabled -> R.drawable.white_background
                YralButtonState.Disabled -> R.drawable.white_background_disabled
                YralButtonState.Loading -> R.drawable.white_background
            }
    }
