package com.yral.shared.features.auth.data

import com.yral.shared.core.AppConfigurations.OAUTH_BASE_URL
import com.yral.shared.features.auth.YralAuthException
import com.yral.shared.features.auth.data.models.AuthClientQuery
import com.yral.shared.features.auth.data.models.PhoneAuthLoginResponseDto
import com.yral.shared.features.auth.data.models.VerifyRequestDto
import com.yral.shared.features.auth.data.models.toExchangePrincipalResponse
import com.yral.shared.features.auth.data.models.toPhoneAuthVerifyResponse
import com.yral.shared.features.auth.data.models.toTokenResponse
import com.yral.shared.features.auth.di.AuthEnv
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.features.auth.domain.models.PhoneAuthLoginResponse
import com.yral.shared.features.auth.domain.models.PhoneAuthVerifyResponse
import com.yral.shared.features.auth.domain.models.TokenResponse
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.rust.service.utils.yralAuthLoginHint
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.json.Json

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val oAuthUtilsHelper: OAuthUtilsHelper,
    private val authEnv: AuthEnv,
    private val json: Json,
) : AuthRepository {
    private var verifier: String = ""

    override suspend fun getOAuthUrl(
        provider: SocialProvider,
        identity: ByteArray,
    ): Pair<Url, String> {
        verifier = oAuthUtilsHelper.generateCodeVerifier()
        val codeChallenge = oAuthUtilsHelper.generateCodeChallenge(verifier)
        val authUrl =
            URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = OAUTH_BASE_URL,
                pathSegments = listOf("oauth", "auth"),
                parameters =
                    Parameters.build {
                        append("provider", provider.value)
                        append("client_id", authEnv.clientId)
                        append("response_type", "code")
                        append("response_mode", "query")
                        append("redirect_uri", authEnv.redirectUri.uriString)
                        append("scope", "openid")
                        append("code_challenge", codeChallenge)
                        append("code_challenge_method", "S256")
                        append("login_hint", yralAuthLoginHint(identity))
                        append("state", codeChallenge)
                    },
            ).build()
        return Pair(authUrl, codeChallenge)
    }

    override suspend fun obtainAnonymousIdentity(): TokenResponse =
        dataSource
            .obtainAnonymousIdentity()
            .toTokenResponse()

    override suspend fun authenticateToken(code: String): TokenResponse =
        dataSource
            .authenticateToken(code, verifier)
            .toTokenResponse()

    override suspend fun refreshToken(token: String): TokenResponse =
        dataSource
            .refreshToken(token)
            .toTokenResponse()

    override suspend fun updateSessionAsRegistered(
        idToken: String,
        canisterId: String,
        userPrincipal: String,
    ) = dataSource
        .updateSessionAsRegistered(idToken, canisterId, userPrincipal)

    override suspend fun exchangePrincipalId(
        idToken: String,
        principalId: String,
    ): ExchangePrincipalResponse =
        dataSource
            .exchangePrincipalId(idToken, principalId)
            .toExchangePrincipalResponse()

    override suspend fun deleteAccount(): String =
        dataSource
            .deleteAccount()

    override suspend fun registerForNotifications(token: String) {
        dataSource.registerForNotifications(token)
    }

    override suspend fun deregisterForNotifications(token: String) {
        dataSource.deregisterForNotifications(token)
    }

    override suspend fun phoneAuthLogin(
        phoneNumber: String,
        identity: ByteArray,
    ): PhoneAuthLoginResponse {
        verifier = oAuthUtilsHelper.generateCodeVerifier()
        val codeChallenge = oAuthUtilsHelper.generateCodeChallenge(verifier)
        val loginHint = yralAuthLoginHint(identity)
        val authClientQuery =
            AuthClientQuery(
                responseType = "code",
                clientId = authEnv.clientId,
                redirectUri = authEnv.redirectUri.uriString,
                state = codeChallenge,
                codeChallenge = codeChallenge,
                codeChallengeMethod = "S256",
                loginHint = json.parseToJsonElement(loginHint),
            )
        return when (val response = dataSource.phoneAuthLogin(phoneNumber, authClientQuery)) {
            is PhoneAuthLoginResponseDto.Success ->
                PhoneAuthLoginResponse(codeChallenge = codeChallenge)
            is PhoneAuthLoginResponseDto.Error -> {
                throw YralAuthException("${response.error} - ${response.errorDescription}")
            }
        }
    }

    override suspend fun verifyPhoneAuth(
        phoneNumber: String,
        code: String,
        clientState: String,
    ): PhoneAuthVerifyResponse =
        dataSource
            .verifyPhoneAuth(
                verifyRequest =
                    VerifyRequestDto(
                        phoneNumber = phoneNumber,
                        code = code,
                        clientState = clientState,
                    ),
            ).toPhoneAuthVerifyResponse()

    override suspend fun createAiAccount(
        userPrincipal: String,
        signature: ByteArray,
        publicKey: ByteArray,
        signedMessage: ByteArray,
        ingressExpirySecs: Long,
        ingressExpiryNanos: Int,
        delegations: List<com.yral.shared.rust.service.utils.SignedDelegationPayload>?,
    ): ByteArray {
        val response =
            dataSource.createAiAccount(
                userPrincipal = userPrincipal,
                signature = signature,
                publicKey = publicKey,
                signedMessage = signedMessage,
                ingressExpirySecs = ingressExpirySecs,
                ingressExpiryNanos = ingressExpiryNanos,
                delegations = delegations,
            )
        return json
            .encodeToString(response.delegatedIdentity)
            .toByteArray()
    }
}
