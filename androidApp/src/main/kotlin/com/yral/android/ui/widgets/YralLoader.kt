package com.yral.android.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieConstants
import com.yral.android.R

@Composable
fun YralLoader(size: Dp = 40.dp) {
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
            rawRes = R.raw.yral_loader,
            iterations = LottieConstants.IterateForever,
        )
    }
}
