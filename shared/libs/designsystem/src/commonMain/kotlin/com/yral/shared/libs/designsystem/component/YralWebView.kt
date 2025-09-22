package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun YralWebView(
    url: String,
    modifier: Modifier = Modifier,
    maxRetries: Int = 3,
    retryDelayMillis: Long = 1000,
)
