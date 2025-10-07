package com.yral.shared.rust.service.domain.models

data class RateLimitStatus(
    val principal: String,
    val windowStart: ULong,
    val isLimited: Boolean,
    val requestCount: ULong,
)
