package com.yral.shared.features.auth.utils

import com.yral.shared.features.auth.domain.models.TokenClaims
import io.ktor.http.Url

internal const val CODE_VERIFIER_LENGTH = 64
internal const val KEY_AUD = "aud" // Client ID
internal const val KEY_EXP = "exp" // Expiry in Epoch seconds, usually 7 days after "iat"
internal const val KEY_IAT = "iat" // Issued At time in Epoch Seconds
internal const val KEY_ISS = "iss" // *Issuer host* usually "yral-auth-v2.fly.dev"
internal const val KEY_SUB = "sub" // Principal Of the Identity
internal const val KEY_NONCE = "nonce" // Optionally set if client set a nonce during authorization code flow
internal const val KEY_IS_ANONYMOUS = "ext_is_anonymous" // Whether this identity anonymous or not
internal const val KEY_DELEGATED_IDENTITY = "ext_delegated_identity" // DelegatedIdentityWire

expect class OAuthUtils() {
    fun openOAuth(
        context: Any,
        authUrl: Url,
        callBack: (code: String, state: String) -> Unit,
    )

    fun invokeCallback(
        code: String,
        state: String,
    )

    internal fun generateCodeVerifier(): String
    internal fun generateCodeChallenge(codeVerifier: String): String
    internal fun generateState(): String
    internal fun parseOAuthToken(token: String): TokenClaims
}
