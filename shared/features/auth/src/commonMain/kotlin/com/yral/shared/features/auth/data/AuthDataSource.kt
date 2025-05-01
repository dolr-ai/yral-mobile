package com.yral.shared.features.auth.data

import com.yral.shared.features.auth.data.models.TokenResponseDto

interface AuthDataSource {
    suspend fun authenticateToken(
        code: String,
        verifier: String,
    ): TokenResponseDto

    suspend fun obtainAnonymousIdentity(): TokenResponseDto
}
