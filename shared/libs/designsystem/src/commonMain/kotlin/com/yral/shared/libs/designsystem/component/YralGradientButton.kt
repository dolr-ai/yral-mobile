package com.yral.shared.libs.designsystem.component

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.pink_gradient_background
import yral_mobile.shared.libs.designsystem.generated.resources.pink_gradient_background_disabled
import yral_mobile.shared.libs.designsystem.generated.resources.transparent_background
import yral_mobile.shared.libs.designsystem.generated.resources.white_background
import yral_mobile.shared.libs.designsystem.generated.resources.white_background_disabled

@Composable
fun YralGradientButton(
    modifier: Modifier = Modifier,
    buttonState: YralButtonState = YralButtonState.Enabled,
    buttonType: YralButtonType = YralButtonType.Pink,
    text: String,
    buttonHeight: Dp = 45.dp,
    fillMaxWidth: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                .height(buttonHeight)
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
        AnimatedVisibility(
            visible = buttonState != YralButtonState.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralMaskedVectorTextV2(
                text = text,
                drawableRes = getButtonTextBackground(buttonType, buttonState),
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
        }
        AnimatedVisibility(
            visible = buttonState == YralButtonState.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.size(20.dp),
                rawRes = getLoaderResource(buttonType),
                iterations = Int.MAX_VALUE,
            )
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
    Transparent,
}

private fun getLoaderResource(buttonType: YralButtonType): LottieRes =
    when (buttonType) {
        YralButtonType.Pink -> LottieRes.WHITE_LOADER
        YralButtonType.White -> LottieRes.YRAL_LOADER
        YralButtonType.Transparent -> LottieRes.YRAL_LOADER
    }

private fun getButtonTextBackground(
    buttonType: YralButtonType,
    buttonState: YralButtonState,
): DrawableResource =
    when (buttonType) {
        YralButtonType.Pink ->
            when (buttonState) {
                YralButtonState.Enabled -> Res.drawable.white_background
                YralButtonState.Disabled -> Res.drawable.white_background_disabled
                YralButtonState.Loading -> Res.drawable.white_background
            }

        YralButtonType.White ->
            when (buttonState) {
                YralButtonState.Enabled -> Res.drawable.pink_gradient_background
                YralButtonState.Disabled -> Res.drawable.pink_gradient_background_disabled
                YralButtonState.Loading -> Res.drawable.pink_gradient_background
            }

        YralButtonType.Transparent ->
            when (buttonState) {
                YralButtonState.Enabled -> Res.drawable.pink_gradient_background
                YralButtonState.Disabled -> Res.drawable.pink_gradient_background_disabled
                YralButtonState.Loading -> Res.drawable.pink_gradient_background
            }
    }

private fun getButtonBackground(
    buttonType: YralButtonType,
    buttonState: YralButtonState,
): DrawableResource =
    when (buttonType) {
        YralButtonType.Pink ->
            when (buttonState) {
                YralButtonState.Enabled -> Res.drawable.pink_gradient_background
                YralButtonState.Disabled -> Res.drawable.pink_gradient_background_disabled
                YralButtonState.Loading -> Res.drawable.pink_gradient_background
            }

        YralButtonType.White ->
            when (buttonState) {
                YralButtonState.Enabled -> Res.drawable.white_background
                YralButtonState.Disabled -> Res.drawable.white_background_disabled
                YralButtonState.Loading -> Res.drawable.white_background
            }

        YralButtonType.Transparent ->
            when (buttonState) {
                YralButtonState.Enabled -> Res.drawable.transparent_background
                YralButtonState.Disabled -> Res.drawable.transparent_background
                YralButtonState.Loading -> Res.drawable.transparent_background
            }
    }
