package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.yral.shared.crashlytics.core.CrashlyticsManager

@Composable
actual fun YralRemoteLottieAnimation(
    modifier: Modifier,
    url: String,
    iterations: Int,
    contentScale: ContentScale,
    onAnimationComplete: () -> Unit,
    onError: (Throwable) -> Unit,
    onLoading: () -> Unit,
    placeholder: @Composable (() -> Unit)?,
    errorContent: @Composable ((Throwable) -> Unit)?,
    crashlyticsManager: CrashlyticsManager,
) {
    onAnimationComplete()
}
