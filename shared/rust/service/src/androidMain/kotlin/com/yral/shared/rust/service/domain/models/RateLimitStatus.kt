package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.RateLimitStatusWrapper

data class RateLimitStatus(
    val principal: String,
    val windowStart: ULong,
    val isLimited: Boolean,
    val requestCount: ULong,
)

internal fun RateLimitStatusWrapper.toStatus() =
    RateLimitStatus(
        principal = principal,
        windowStart = windowStart,
        isLimited = isLimited,
        requestCount = requestCount,
    )
