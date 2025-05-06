package com.yral.shared.features.auth

import com.yral.shared.features.auth.utils.SocialProvider

interface AuthClient {
    suspend fun initialize()
    suspend fun signInWithSocial(provider: SocialProvider)
    suspend fun handleOAuthCallback(
        code: String,
        state: String,
    )
    suspend fun logout()
}
