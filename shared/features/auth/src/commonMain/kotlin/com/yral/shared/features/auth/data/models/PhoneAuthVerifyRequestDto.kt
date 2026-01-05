package com.yral.shared.features.auth.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyRequestDto(
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("code") val code: String,
    @SerialName("client_state") val clientState: String,
)

@Serializable
data class PhoneAuthVerifyRequestDto(
    @SerialName("verify_request") val verifyRequest: VerifyRequestDto,
)
