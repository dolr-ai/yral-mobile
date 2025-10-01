package com.yral.android.ui.screens.btcRewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.screens.btcRewards.BtcRewardsBottomSheetConstants.ANIMATION_VIEW_ASPECT_RATIO
import com.yral.android.ui.screens.btcRewards.nav.BtcRewardsComponent
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralBrushes
import com.yral.shared.libs.designsystem.theme.YralColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BtcRewardsBottomSheet(component: BtcRewardsComponent) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    YralBottomSheet(
        onDismissRequest = { component.onDismissClicked() },
        bottomSheetState = bottomSheetState,
        dragHandle = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(46.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 36.dp),
        ) {
            Text(
                text = stringResource(R.string.btc_credited),
                style = LocalAppTopography.current.lgBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(ANIMATION_VIEW_ASPECT_RATIO)) {
                YralLottieAnimation(
                    rawRes = LottieRes.BTC_CREDITED,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Inside,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = buildAnnotatedCongratsText(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
                    horizontalAlignment = Alignment.Start,
                ) {
                    YralGradientButton(
                        text = stringResource(R.string.go_to_wallet),
                    ) { component.openWallet() }
                    YralButton(
                        text = stringResource(R.string.keep_scrolling),
                        borderColor = YralColors.Neutral700,
                        borderWidth = 1.dp,
                        backgroundColor = YralColors.Neutral800,
                        textStyle =
                            TextStyle(
                                color = YralColors.NeutralTextPrimary,
                            ),
                    ) { component.openFeed() }
                }
            }
        }
    }
}

@Composable
private fun buildAnnotatedCongratsText(): AnnotatedString =
    buildAnnotatedString {
        val fullText = stringResource(R.string.btc_rewards_congratulations)
        val maskedText = stringResource(R.string.bit_coin)
        val maskedStart = fullText.indexOf(maskedText)
        val maskedEnd = maskedStart + maskedText.length
        val textStyle = LocalAppTopography.current.mdBold
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
            )
        if (maskedStart >= 0) {
            withStyle(
                style = spanStyle.copy(color = YralColors.Neutral300),
            ) { append(fullText.substring(0, maskedStart)) }

            withStyle(
                style = spanStyle.copy(brush = YralBrushes.GoldenTextBrush),
            ) { append(fullText.substring(maskedStart, maskedEnd)) }

            if (maskedEnd < fullText.length) {
                withStyle(
                    style = spanStyle.copy(color = YralColors.Neutral300),
                ) { append(fullText.substring(maskedEnd)) }
            }
        } else {
            withStyle(
                style = spanStyle.copy(color = YralColors.Neutral300),
            ) {
                append(fullText)
            }
        }
    }

object BtcRewardsBottomSheetConstants {
    const val ANIMATION_VIEW_ASPECT_RATIO = 2.2f
}
