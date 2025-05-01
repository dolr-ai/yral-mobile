package com.yral.shared.features.auth.data

import com.yral.shared.features.auth.data.models.TokenResponseDto
import io.ktor.http.Cookie

interface AuthDataSource {
    suspend fun setAnonymousIdentityCookie()
    suspend fun extractIdentity(cookie: Cookie): ByteArray
    suspend fun authenticateToken(
        code: String,
        verifier: String,
    ): TokenResponseDto
    suspend fun getAnonymousIdentityCookie(): Cookie?
}
