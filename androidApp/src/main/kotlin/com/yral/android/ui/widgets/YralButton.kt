package com.yral.android.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieConstants
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors

@Composable
fun YralButton(
    text: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    textStyle: TextStyle? = null,
    backgroundColor: Color = Color.White,
    borderWidth: Dp = 0.75.dp,
    borderColor: Color = YralColors.ButtonBorderColor,
    icon: Int? = null,
    buttonState: YralButtonState = YralButtonState.Enabled,
    loader: Int = R.raw.pink_loader,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .height(45.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp),
                ).shadow(
                    elevation = 22.5.dp,
                    spotColor = YralColors.ShadowSpotColor,
                    ambientColor = YralColors.ShadowAmbientColor,
                ).border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(size = 8.dp),
                ).padding(all = 10.dp)
                .clickable {
                    onClick()
                },
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Image(
                painter = painterResource(id = icon),
                contentDescription = "image description",
                contentScale = ContentScale.None,
            )
        }
        val defaultStyle =
            LocalAppTopography
                .current
                .mdBold
                .copy(
                    color = YralColors.NeutralBlack,
                    textAlign = TextAlign.Center,
                )
        Text(
            text = text,
            style =
                textStyle?.let {
                    defaultStyle.plus(it)
                } ?: defaultStyle,
        )
        AnimatedVisibility(
            visible = buttonState == YralButtonState.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.size(20.dp),
                rawRes = loader,
                iterations = LottieConstants.IterateForever,
            )
        }
    }
}
