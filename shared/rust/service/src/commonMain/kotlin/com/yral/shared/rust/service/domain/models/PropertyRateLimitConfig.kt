package com.yral.shared.rust.service.domain.models

data class PropertyRateLimitConfig(
    val propertyRateLimitWindowDurationSeconds: ULong?,
    val windowDurationSeconds: ULong,
    val maxRequestsPerWindowRegistered: ULong,
    val maxRequestsPerPropertyAllUsers: ULong?,
    val property: String,
    val maxRequestsPerWindowUnregistered: ULong,
)
