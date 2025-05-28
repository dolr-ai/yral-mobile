package com.yral.shared.features.auth.domain.models

data class TokenResponse(
    val idToken: String,
    val accessToken: String,
    val expiresIn: Long,
    val refreshToken: String,
    val tokenType: String,
)
