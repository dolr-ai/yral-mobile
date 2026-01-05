package com.yral.shared.features.auth

import com.yral.shared.core.session.Session
import com.yral.shared.features.auth.domain.models.PhoneAuthVerifyResponse
import com.yral.shared.features.auth.utils.SocialProvider

interface AuthClient {
    suspend fun initialize()
    suspend fun signInWithSocial(
        context: Any,
        provider: SocialProvider,
    )
    suspend fun logout()
    suspend fun authorizeFirebase(session: Session)
    suspend fun fetchBalance(session: Session)
    suspend fun phoneAuthLogin(phoneNumber: String): String
    suspend fun verifyPhoneAuth(
        phoneNumber: String,
        code: String,
    ): PhoneAuthVerifyResponse
}
