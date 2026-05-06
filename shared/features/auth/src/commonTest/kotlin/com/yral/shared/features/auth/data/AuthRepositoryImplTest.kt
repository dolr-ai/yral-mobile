package com.yral.shared.features.auth.data

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.data.models.AuthClientQuery
import com.yral.shared.features.auth.data.models.CreateAiAccountResponseDto
import com.yral.shared.features.auth.data.models.ExchangePrincipalResponseDto
import com.yral.shared.features.auth.data.models.PhoneAuthLoginResponseDto
import com.yral.shared.features.auth.data.models.PhoneAuthVerifyResponseDto
import com.yral.shared.features.auth.data.models.TokenResponseDto
import com.yral.shared.features.auth.data.models.VerifyRequestDto
import com.yral.shared.features.auth.di.AuthEnv
import com.yral.shared.features.auth.domain.models.TokenClaims
import com.yral.shared.features.auth.utils.OAuthResult
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.rust.service.utils.SignedDelegationPayload
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRepositoryImplTest {
    private val authEnv =
        AuthEnv(
            clientId = "test-client-id",
            redirectUri = AuthEnv.RedirectUri(scheme = "yral"),
            notificationEnvironment = "staging",
        )

    @Test
    fun getOAuthUrl_usesCurrentAuthHost() =
        runTest {
            val resolver = SessionAuthHostResolver(CrashlyticsManager())
            val repository = createRepository(resolver)

            val initialUrl = repository.getOAuthUrl(SocialProvider.GOOGLE, byteArrayOf(1)).first
            resolver.activateFallback("auth.dolr.ai")
            val fallbackUrl = repository.getOAuthUrl(SocialProvider.GOOGLE, byteArrayOf(1)).first

            assertEquals("auth.dolr.ai", initialUrl.host)
            assertEquals("auth.yral.com", fallbackUrl.host)
        }

    private fun createRepository(resolver: SessionAuthHostResolver) =
        AuthRepositoryImpl(
            dataSource = FakeAuthDataSource(),
            oAuthUtilsHelper = FakeOAuthUtilsHelper(),
            authEnv = authEnv,
            json = Json,
            authHostResolver = resolver,
            authLoginHintProvider = FakeAuthLoginHintProvider(),
        )

    private class FakeAuthLoginHintProvider : AuthLoginHintProvider {
        override fun build(identity: ByteArray): String = "login-hint"
    }

    private class FakeOAuthUtilsHelper : OAuthUtilsHelper {
        override fun generateCodeVerifier(): String = "verifier"

        override fun generateCodeChallenge(codeVerifier: String): String = "challenge"

        override fun generateState(): String = "state"

        override fun parseOAuthToken(token: String): TokenClaims {
            error("Not used in this test")
        }

        override fun mapUriToOAuthResult(uri: String): OAuthResult? = null
    }

    private class FakeAuthDataSource : AuthDataSource {
        override suspend fun obtainAnonymousIdentity(): TokenResponseDto = error("Not used in this test")

        override suspend fun authenticateToken(
            code: String,
            verifier: String,
        ): TokenResponseDto = error("Not used in this test")

        override suspend fun refreshToken(token: String): TokenResponseDto = error("Not used in this test")

        override suspend fun updateSessionAsRegistered(
            idToken: String,
            canisterId: String,
            userPrincipal: String,
        ) = error("Not used in this test")

        override suspend fun exchangePrincipalId(
            idToken: String,
            principalId: String,
        ): ExchangePrincipalResponseDto = error("Not used in this test")

        override suspend fun deleteAccount(): String = error("Not used in this test")

        override suspend fun registerForNotifications(token: String) = error("Not used in this test")

        override suspend fun deregisterForNotifications(token: String) = error("Not used in this test")

        override suspend fun phoneAuthLogin(
            phoneNumber: String,
            authClientQuery: AuthClientQuery,
        ): PhoneAuthLoginResponseDto = error("Not used in this test")

        override suspend fun verifyPhoneAuth(verifyRequest: VerifyRequestDto): PhoneAuthVerifyResponseDto = error("Not used in this test")

        override suspend fun createAiAccount(
            userPrincipal: String,
            signature: ByteArray,
            publicKey: ByteArray,
            signedMessage: ByteArray,
            ingressExpirySecs: Long,
            ingressExpiryNanos: Int,
            delegations: List<SignedDelegationPayload>?,
        ): CreateAiAccountResponseDto = error("Not used in this test")
    }
}
