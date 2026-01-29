package com.yral.shared.features.subscriptions.nav

import androidx.compose.runtime.Composable

/**
 * Reusable content for the subscription nudge bottomsheet: [title], [description],
 * and optional [topContent] (e.g. image). Held in memory by the coordinator when
 * the nudge is shown; not serializable.
 */
data class SubscriptionNudgeContent(
    val title: String,
    val description: String,
    val topContent: @Composable () -> Unit = {},
)
