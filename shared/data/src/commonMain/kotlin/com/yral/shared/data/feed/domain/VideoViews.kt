package com.yral.shared.data.feed.domain

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class VideoViews(
    val videoId: String,
    val allViews: ULong,
    val loggedInViews: ULong,
    val lastFetched: Instant,
)
