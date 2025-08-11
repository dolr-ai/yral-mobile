package com.yral.shared.features.auth

import com.yral.shared.features.auth.utils.SocialProvider

interface AuthClient {
    suspend fun initialize()
    suspend fun signInWithSocial(
        context: Any,
        provider: SocialProvider,
    )
    suspend fun logout()
}
