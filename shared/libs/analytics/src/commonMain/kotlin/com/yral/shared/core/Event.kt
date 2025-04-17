package com.yral.shared.core

import kotlinx.datetime.Clock

data class Event(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
)
