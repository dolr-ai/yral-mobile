package com.yral.shared.rust.service.data.models

import com.yral.shared.rust.service.domain.models.PropertyRateLimitConfig
import com.yral.shared.uniffi.generated.PropertyRateLimitConfigWrapper

internal fun PropertyRateLimitConfigWrapper.toConfig() =
    PropertyRateLimitConfig(
        propertyRateLimitWindowDurationSeconds = propertyRateLimitWindowDurationSeconds,
        windowDurationSeconds = windowDurationSeconds,
        maxRequestsPerWindowRegistered = maxRequestsPerWindowRegistered,
        maxRequestsPerPropertyAllUsers = maxRequestsPerPropertyAllUsers,
        property = property,
        maxRequestsPerWindowUnregistered = maxRequestsPerWindowUnregistered,
    )
