package com.yral.shared.rust.service.data.models

import com.yral.shared.rust.service.domain.models.RateLimitStatus
import com.yral.shared.uniffi.generated.RateLimitStatusWrapper

internal fun RateLimitStatusWrapper.toStatus() =
    RateLimitStatus(
        principal = principal,
        windowStart = windowStart,
        isLimited = isLimited,
        requestCount = requestCount,
    )
