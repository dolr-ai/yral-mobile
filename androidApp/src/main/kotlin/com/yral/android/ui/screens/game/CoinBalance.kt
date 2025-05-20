package com.yral.android.ui.screens.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors

@Suppress("LongMethod")
@Composable
fun CoinBalance(
    coinBalance: Long,
    coinDelta: Int,
    animateBag: Boolean,
    setAnimate: (Boolean) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .padding(top = 22.dp, bottom = 22.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier =
                Modifier
                    .height(32.dp)
                    .background(
                        brush =
                            Brush.linearGradient(
                                listOf(
                                    YralColors.coinBalanceBGStart,
                                    YralColors.coinBalanceBGEnd,
                                ),
                            ),
                        shape = RoundedCornerShape(16.dp),
                    ).padding(start = 22.dp, end = 10.dp)
                    .align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = coinBalance.toString(),
                style = LocalAppTopography.current.feedCanisterId,
                color = YralColors.PrimaryContainer,
            )
        }
        if (!animateBag) {
            Image(
                painter = painterResource(R.drawable.coin_bag),
                contentDescription = "coin bag",
                modifier =
                    Modifier
                        .size(36.dp)
                        .offset(x = (-22).dp),
            )
        } else {
            val bagAnimationRes =
                if (coinDelta > 0) R.raw.smiley_game_win else R.raw.smiley_game_lose
            val bagAnimationComposition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(
                    bagAnimationRes,
                ),
            )
            val bagAnimationProgress by animateLottieCompositionAsState(
                composition = bagAnimationComposition,
                iterations = 1,
                isPlaying = true, // Only start playing when initial animation is complete
            )
            LottieAnimation(
                modifier =
                    Modifier
                        .height(52.dp)
                        .width(105.dp)
                        .offset(x = (-22).dp)
                        .graphicsLayer {
                            clip = false
                        },
                composition = bagAnimationComposition,
                progress = { bagAnimationProgress },
            )
            LaunchedEffect(bagAnimationProgress) {
                if (bagAnimationProgress == 1f) {
                    setAnimate(false)
                }
            }
        }
    }
}
