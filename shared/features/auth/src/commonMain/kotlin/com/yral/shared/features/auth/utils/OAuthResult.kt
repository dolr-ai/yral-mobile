package com.yral.shared.features.auth.utils

sealed class OAuthResult {
    data class Success(
        val code: String,
        val state: String,
    ) : OAuthResult()

    data class Error(
        val error: String,
        val errorDescription: String? = null,
    ) : OAuthResult()

    data object Cancelled : OAuthResult()

    data object TimedOut : OAuthResult()
}
