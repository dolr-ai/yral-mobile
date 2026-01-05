package com.yral.shared.features.auth.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PhoneAuthLoginResponseDto {
    @Serializable
    object Success : PhoneAuthLoginResponseDto()

    @Serializable
    data class Error(
        @SerialName("error") val error: Map<String, String>,
        @SerialName("error_description") val errorDescription: String,
    ) : PhoneAuthLoginResponseDto()
}
