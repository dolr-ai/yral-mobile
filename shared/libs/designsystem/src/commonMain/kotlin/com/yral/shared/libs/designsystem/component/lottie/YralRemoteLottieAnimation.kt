package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.yral.shared.crashlytics.core.CrashlyticsManager
import org.koin.compose.koinInject

@Composable
expect fun YralRemoteLottieAnimation(
    modifier: Modifier = Modifier.Companion,
    url: String,
    iterations: Int = Int.MAX_VALUE,
    contentScale: ContentScale = ContentScale.Companion.FillBounds,
    onAnimationComplete: () -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onLoading: () -> Unit = {},
    placeholder: @Composable (() -> Unit)? = null,
    errorContent: @Composable ((Throwable) -> Unit)? = null,
    crashlyticsManager: CrashlyticsManager = koinInject(),
)
