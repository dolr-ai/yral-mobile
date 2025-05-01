package com.yral.shared.features.auth.domain

import com.yral.shared.features.auth.domain.models.TokenResponse
import com.yral.shared.features.auth.utils.SocialProvider
import io.ktor.http.Cookie
import io.ktor.http.Url

interface AuthRepository {
    suspend fun setAnonymousIdentityCookie()
    suspend fun extractIdentity(cookie: Cookie): ByteArray
    suspend fun getOAuthUrl(
        provider: SocialProvider,
        identity: ByteArray,
    ): Pair<Url, String>
    suspend fun authenticateToken(code: String): TokenResponse
    suspend fun getAnonymousIdentityCookie(): Cookie?
}
