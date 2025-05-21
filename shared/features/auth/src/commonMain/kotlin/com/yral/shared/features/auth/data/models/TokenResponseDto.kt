package com.yral.shared.features.auth.data.models

import com.yral.shared.features.auth.domain.models.TokenResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponseDto(
    @SerialName("id_token")
    val idToken: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("token_type")
    val tokenType: String,
)

fun TokenResponseDto.toTokenResponse(): TokenResponse =
    TokenResponse(
        idToken = idToken,
        accessToken = accessToken,
        expiresIn = expiresIn,
        refreshToken = refreshToken,
        tokenType = tokenType,
    )
