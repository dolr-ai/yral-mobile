package com.yral.shared.features.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.features.game.viewmodel.HotOrNotVoteResult
import com.yral.shared.libs.designsystem.component.YralFeedback
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.flop_icon
import yral_mobile.shared.features.game.generated.resources.hit_icon
import yral_mobile.shared.features.game.generated.resources.hot_or_not_loss
import yral_mobile.shared.features.game.generated.resources.hot_or_not_win
import yral_mobile.shared.features.game.generated.resources.you_lost_x_coins
import yral_mobile.shared.features.game.generated.resources.you_win_x_coins
import kotlin.math.abs

private const val RESULT_ICON_SIZE = 64

@Composable
fun HotOrNotResultOverlay(
    result: HotOrNotVoteResult,
    hasShownAnimation: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val voteResult = result.voteResult
    val isWin = voteResult.coinDelta > 0

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Result strip at bottom
        HotOrNotResultStrip(
            isHot = result.isHot,
            coinDelta = voteResult.coinDelta,
            errorMessage = voteResult.errorMessage,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Coin delta animation overlay
        if (!hasShownAnimation && voteResult.errorMessage.isEmpty()) {
            CoinDeltaAnimation(
                text = voteResult.coinDelta.toSignedString(),
                textColor =
                    if (isWin) {
                        YralColors.Green300.copy(alpha = 0.3f)
                    } else {
                        YralColors.Red300.copy(alpha = 0.3f)
                    },
                onAnimationEnd = onAnimationComplete,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // Sound and haptic feedback
    if (!hasShownAnimation && voteResult.coinDelta != 0) {
        YralFeedback(
            soundUri = if (isWin) spilledCoinSoundUri() else coinLossSoundUri(),
            withHapticFeedback = true,
            hapticFeedbackType = HapticFeedbackType.LongPress,
        )
    }
}

@Composable
private fun HotOrNotResultStrip(
    isHot: Boolean,
    coinDelta: Int,
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    GameStripBackground(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
    ) {
        // Show Hit or Flop icon based on user's choice
        Image(
            painter =
                painterResource(
                    if (isHot) Res.drawable.hit_icon else Res.drawable.flop_icon,
                ),
            contentDescription = if (isHot) "Hit" else "Flop",
            modifier = Modifier.size(RESULT_ICON_SIZE.dp),
        )

        // Result text
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = hotOrNotResultText(coinDelta, errorMessage),
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun hotOrNotResultText(
    coinDelta: Int,
    errorMessage: String,
) = buildAnnotatedString {
    val textStyle = LocalAppTopography.current.mdBold
    val baseSpanStyle =
        SpanStyle(
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
            color = YralColors.Green50,
        )

    if (errorMessage.isNotEmpty()) {
        withStyle(baseSpanStyle.copy(color = YralColors.Red300)) {
            append(errorMessage)
        }
        return@buildAnnotatedString
    }

    val isWin = coinDelta > 0
    if (isWin) {
        withStyle(baseSpanStyle) {
            append(stringResource(Res.string.hot_or_not_win))
            append(" ")
        }
        withStyle(baseSpanStyle.copy(color = YralColors.Green300)) {
            append(stringResource(Res.string.you_win_x_coins, coinDelta))
        }
    } else {
        withStyle(baseSpanStyle) {
            append(stringResource(Res.string.hot_or_not_loss))
            append(" ")
        }
        withStyle(baseSpanStyle.copy(color = YralColors.Red300)) {
            append(stringResource(Res.string.you_lost_x_coins, abs(coinDelta)))
        }
    }
}

private fun Int.toSignedString(): String =
    if (this >= 0) {
        "+$this"
    } else {
        "$this"
    }
