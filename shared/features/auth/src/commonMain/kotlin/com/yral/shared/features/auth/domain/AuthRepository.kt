package com.yral.shared.features.auth.domain

import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.features.auth.domain.models.TokenResponse
import com.yral.shared.features.auth.utils.SocialProvider
import io.ktor.http.Url

interface AuthRepository {
    suspend fun getOAuthUrl(
        provider: SocialProvider,
        identity: ByteArray,
    ): Pair<Url, String>

    suspend fun obtainAnonymousIdentity(): TokenResponse
    suspend fun authenticateToken(code: String): TokenResponse
    suspend fun refreshToken(token: String): TokenResponse
    suspend fun updateSessionAsRegistered(
        idToken: String,
        canisterId: String,
    )
    suspend fun exchangePrincipalId(
        idToken: String,
        principalId: String,
    ): ExchangePrincipalResponse
    suspend fun deleteAccount(): String
}
