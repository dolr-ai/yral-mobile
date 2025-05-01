package com.yral.shared.features.auth

interface AuthClient {
    suspend fun initialize()
    suspend fun refreshAuthIfNeeded()
    suspend fun signInWithSocial(provider: SocialProvider)
    fun handleOAuthCallback(
        code: String,
        state: String,
    )
}
