package com.yral.shared.features.auth.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuthClientQuery(
    @SerialName("response_type") val responseType: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("redirect_uri") val redirectUri: String,
    @SerialName("state") val state: String,
    @SerialName("code_challenge") val codeChallenge: String,
    @SerialName("code_challenge_method") val codeChallengeMethod: String,
    @SerialName("login_hint") val loginHint: JsonElement,
)

@Serializable
data class PhoneAuthLoginRequestDto(
    @SerialName("auth_client_query") val authClientQuery: AuthClientQuery,
    @SerialName("phone_number") val phoneNumber: String,
)
