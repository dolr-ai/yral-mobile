package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import com.yral.shared.crashlytics.core.CrashlyticsManager

@Composable
actual fun PreloadLottieAnimation(
    url: String,
    crashlyticsManager: CrashlyticsManager,
) {
    // no-op
}
