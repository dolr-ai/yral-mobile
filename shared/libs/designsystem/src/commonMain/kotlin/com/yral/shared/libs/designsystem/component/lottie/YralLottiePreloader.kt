package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import com.yral.shared.crashlytics.core.CrashlyticsManager
import org.koin.compose.koinInject

@Composable
expect fun PreloadLottieAnimation(
    url: String,
    crashlyticsManager: CrashlyticsManager = koinInject(),
)

@Composable
fun PreloadLottieAnimations(urls: List<String>) {
    urls.forEach { url -> PreloadLottieAnimation(url) }
}
