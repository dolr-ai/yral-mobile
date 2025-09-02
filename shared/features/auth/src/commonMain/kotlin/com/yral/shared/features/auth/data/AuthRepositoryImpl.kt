package com.yral.shared.features.auth.data

import com.yral.shared.core.AppConfigurations.OAUTH_BASE_URL
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.CLIENT_ID
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI
import com.yral.shared.features.auth.data.models.toExchangePrincipalResponse
import com.yral.shared.features.auth.data.models.toTokenResponse
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.features.auth.domain.models.TokenResponse
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.rust.service.utils.yralAuthLoginHint
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val oAuthUtilsHelper: OAuthUtilsHelper,
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
                        append("client_id", CLIENT_ID)
                        append("response_type", "code")
                        append("response_mode", "query")
                        append("redirect_uri", REDIRECT_URI)
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
    ) = dataSource
        .updateSessionAsRegistered(idToken, canisterId)

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
}
