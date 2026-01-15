package com.yral.shared.features.auth.data

import com.yral.shared.features.auth.data.models.AuthClientQuery
import com.yral.shared.features.auth.data.models.ExchangePrincipalResponseDto
import com.yral.shared.features.auth.data.models.PhoneAuthLoginResponseDto
import com.yral.shared.features.auth.data.models.PhoneAuthVerifyResponseDto
import com.yral.shared.features.auth.data.models.TokenResponseDto
import com.yral.shared.features.auth.data.models.VerifyRequestDto

interface AuthDataSource {
    suspend fun obtainAnonymousIdentity(): TokenResponseDto
    suspend fun authenticateToken(
        code: String,
        verifier: String,
    ): TokenResponseDto
    suspend fun refreshToken(token: String): TokenResponseDto
    suspend fun updateSessionAsRegistered(
        idToken: String,
        canisterId: String,
        userPrincipal: String,
    )
    suspend fun exchangePrincipalId(
        idToken: String,
        principalId: String,
    ): ExchangePrincipalResponseDto
    suspend fun deleteAccount(): String
    suspend fun registerForNotifications(token: String)
    suspend fun deregisterForNotifications(token: String)
    suspend fun phoneAuthLogin(
        phoneNumber: String,
        authClientQuery: AuthClientQuery,
    ): PhoneAuthLoginResponseDto
    suspend fun verifyPhoneAuth(verifyRequest: VerifyRequestDto): PhoneAuthVerifyResponseDto
}
