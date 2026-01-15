package com.yral.shared.features.auth.domain.models

sealed class PhoneAuthVerifyResponse {
    data class Success(
        val idTokenCode: String,
        val redirectUri: String,
    ) : PhoneAuthVerifyResponse()

    data class Error(
        val error: String,
        val errorDescription: String,
        val errorMessage: String,
    ) : PhoneAuthVerifyResponse()
}
