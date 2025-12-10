package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation

@Composable
fun YralLoader(
    size: Dp = 40.dp,
    resource: LottieRes = LottieRes.YRAL_LOADER,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        YralLottieAnimation(
            modifier =
                Modifier
                    .size(size),
            rawRes = resource,
            iterations = Int.MAX_VALUE,
        )
    }
}

@Composable
fun YralLoadingDots(
    size: DpSize = DpSize(30.dp, 20.dp),
    resource: LottieRes = LottieRes.LOADING_DOTS,
) {
    YralLottieAnimation(
        modifier = Modifier.size(size),
        rawRes = resource,
        iterations = Int.MAX_VALUE,
    )
}
