package com.yral.shared.features.auth.data.models

import com.yral.shared.features.auth.domain.models.PhoneAuthVerifyResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PhoneAuthVerifyResponseDto {
    @Serializable
    data class Success(
        @SerialName("id_token_code") val idTokenCode: String,
        @SerialName("redirect_uri") val redirectUri: String,
    ) : PhoneAuthVerifyResponseDto()

    @Serializable
    data class Error(
        @SerialName("error") val error: String,
        @SerialName("error_description") val errorDescription: String,
    ) : PhoneAuthVerifyResponseDto()
}

fun PhoneAuthVerifyResponseDto.toPhoneAuthVerifyResponse(): PhoneAuthVerifyResponse =
    when (this) {
        is PhoneAuthVerifyResponseDto.Success ->
            PhoneAuthVerifyResponse.Success(
                idTokenCode = idTokenCode,
                redirectUri = redirectUri,
            )

        is PhoneAuthVerifyResponseDto.Error -> {
            PhoneAuthVerifyResponse.Error(
                error = error,
                errorDescription = errorDescription,
                errorMessage = "$error - $errorDescription",
            )
        }
    }
