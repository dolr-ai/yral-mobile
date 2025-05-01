package com.yral.shared.features.auth

import android.net.Uri
import com.yral.shared.core.platform.PlatformResourcesFactory

const val CODE_VERIFIER_LENGTH = 64
const val KEY_AUD = "aud" // Client ID
const val KEY_EXP = "exp" // Expiry in Epoch seconds, usually 7 days after "iat"
const val KEY_IAT = "iat" // Issued At time in Epoch Seconds
const val KEY_ISS = "iss" // *Issuer host* usually "yral-auth-v2.fly.dev"
const val KEY_SUB = "sub" // Principal Of the Identity
const val KEY_NONCE = "nonce" // Optionally set if client set a nonce during authorization code flow
const val KEY_IS_ANONYMOUS = "ext_is_anonymous" // Whether this identity anonymous or not
const val KEY_DELEGATED_IDENTITY = "ext_delegated_identity" // DelegatedIdentityWire

expect fun openOAuth(
    platformResourcesFactory: PlatformResourcesFactory,
    authUri: Uri,
)

expect fun generateCodeVerifier(): String

expect fun generateCodeChallenge(codeVerifier: String): String

expect fun generateState(): String

expect fun parseAccessTokenForIdentity(accessToken: String): ByteArray
