package com.yral.android.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.YralButton
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.shared.analytics.events.GameConcludedCtaType
import com.yral.shared.features.game.domain.models.GameIcon
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameResultSheet(
    gameIcon: GameIcon?,
    coinDelta: Int,
    onDismissRequest: () -> Unit,
    openAboutGame: () -> Unit,
    onSheetButtonClicked: (GameConcludedCtaType) -> Unit,
) {
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
        dragHandle = {
            DragHandle(
                color = YralColors.Neutral500,
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    text =
                        stringResource(
                            if (coinDelta > 0) {
                                R.string.congratulations
                            } else {
                                R.string.oops
                            },
                        ),
                    style = LocalAppTopography.current.lgBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                GameResultBagAnimation(coinDelta)
                GameResultSheetMessage(
                    coinDelta = coinDelta,
                    gameIcon = gameIcon,
                )
            }
            GameResultSheetButtons(
                onDismissRequest = onDismissRequest,
                openAboutGame = openAboutGame,
                onSheetButtonClicked = onSheetButtonClicked,
            )
        }
    }
}

@Composable
private fun GameResultSheetMessage(
    coinDelta: Int,
    gameIcon: GameIcon?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp,
                    Alignment.CenterHorizontally,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = getResultText(coinDelta),
                style = LocalAppTopography.current.mdMedium,
                color = YralColors.Green50,
                textAlign = TextAlign.Center,
            )
            gameIcon?.let {
                GameIcon(
                    modifier = Modifier.size(26.dp),
                    icon = it,
                )
            }
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = getResultCentsText(coinDelta),
            style = LocalAppTopography.current.mdMedium,
            color = getResultCentsTextColor(coinDelta),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun getResultText(coinDelta: Int): String =
    stringResource(
        if (coinDelta > 0) {
            R.string.since_most_people_voted_on
        } else {
            R.string.since_most_people_not_voted_on
        },
    )

@Composable
private fun getResultCentsText(coinDelta: Int): String =
    stringResource(
        if (coinDelta > 0) {
            R.string.you_win_x_coins
        } else {
            R.string.you_lost_x_coins
        },
        abs(coinDelta),
    )

@Composable
private fun getResultCentsTextColor(coinDelta: Int): Color =
    if (coinDelta > 0) {
        YralColors.Green300
    } else {
        YralColors.Red300
    }

@Composable
private fun GameResultBagAnimation(coinDelta: Int) {
    val bagAnimationRes =
        if (coinDelta > 0) R.raw.smiley_game_win else R.raw.smiley_game_lose
    YralLottieAnimation(
        rawRes = bagAnimationRes,
        iterations = 1,
        modifier =
            Modifier
                .width(250.dp)
                .height(130.dp),
    )
}

@Composable
private fun GameResultSheetButtons(
    onDismissRequest: () -> Unit,
    openAboutGame: () -> Unit,
    onSheetButtonClicked: (GameConcludedCtaType) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
    ) {
        YralGradientButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.keep_playing),
        ) {
            onSheetButtonClicked(GameConcludedCtaType.KEEP_PLAYING)
            onDismissRequest()
        }
        YralButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.learn_more),
            borderWidth = 1.dp,
            borderColor = YralColors.Pink300,
            backgroundColor = YralColors.Neutral900,
            textStyle =
                TextStyle(
                    color = YralColors.Pink300,
                ),
        ) {
            onSheetButtonClicked(GameConcludedCtaType.LEARN_MORE)
            openAboutGame()
        }
    }
}
