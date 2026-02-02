package com.yral.shared.features.subscriptions.nav

import androidx.compose.runtime.Composable

/**
 * Reusable content for the subscription nudge bottomsheet:
 * optional [title], optional [description] and optional [topContent].
 * When [title] is null the sheet shows generic "Go Pro" title;
 * When [description] is null it shows generic benefit rows.
 */
data class SubscriptionNudgeContent(
    val title: String? = null,
    val description: String? = null,
    val topContent: @Composable () -> Unit = {},
)
