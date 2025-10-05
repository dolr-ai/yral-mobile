package com.yral.shared.features.wallet.ui.btcRewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.features.wallet.ui.btcRewards.nav.VideoViewRewardsComponent
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralBrushes
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.wallet.generated.resources.Res
import yral_mobile.shared.features.wallet.generated.resources.bit_coin
import yral_mobile.shared.features.wallet.generated.resources.btc_credited
import yral_mobile.shared.features.wallet.generated.resources.btc_rewards_congratulations
import yral_mobile.shared.features.wallet.generated.resources.go_to_wallet
import yral_mobile.shared.features.wallet.generated.resources.keep_scrolling

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewsRewardsBottomSheet(component: VideoViewRewardsComponent) {
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
                text = stringResource(Res.string.btc_credited),
                style = LocalAppTopography.current.lgBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.width(358.dp).height(161.dp)) {
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
                        text = stringResource(Res.string.go_to_wallet),
                    ) { component.openWallet() }
                    YralButton(
                        text = stringResource(Res.string.keep_scrolling),
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
        val fullText = stringResource(Res.string.btc_rewards_congratulations)
        val maskedText = stringResource(Res.string.bit_coin)
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
