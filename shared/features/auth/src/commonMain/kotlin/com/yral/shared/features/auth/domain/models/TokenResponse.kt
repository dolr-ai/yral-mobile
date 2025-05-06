package com.yral.shared.features.auth.domain.models

data class TokenResponse(
    val accessToken: String,
    val expiresIn: Long,
    val refreshToken: String,
    val tokenType: String,
)
