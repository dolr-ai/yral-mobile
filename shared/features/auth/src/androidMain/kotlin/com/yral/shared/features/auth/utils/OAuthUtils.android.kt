package com.yral.shared.features.auth.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jwt.JWTParser
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.features.auth.domain.models.TokenClaims
import io.ktor.http.Url
import io.ktor.http.toURI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date

@SuppressLint("UseKtx")
actual fun openOAuth(
    platformResourcesFactory: PlatformResourcesFactory,
    authUrl: Url,
) {
    val customTabsIntent =
        CustomTabsIntent
            .Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .build()

    // Add flags to keep the app in the foreground
    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

    // Launch the custom tab
    customTabsIntent
        .launchUrl(
            platformResourcesFactory.resources().activityContext,
            Uri.parse(authUrl.toURI().toString()),
        )
}

actual fun generateCodeVerifier(): String {
    val secureRandom = SecureRandom()
    val bytes = ByteArray(CODE_VERIFIER_LENGTH)
    secureRandom.nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}

actual fun generateCodeChallenge(codeVerifier: String): String {
    val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(bytes)
    val digest = messageDigest.digest()
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}

actual fun generateState(): String {
    val random = SecureRandom()
    val bytes = ByteArray(CODE_VERIFIER_LENGTH)
    random.nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}

actual fun parseOAuthToken(token: String): TokenClaims {
    val jwt = JWTParser.parse(token)
    val claims = jwt.jwtClaimsSet.claims
    val gson = Gson()
    return TokenClaims(
        aud = claims[KEY_AUD] as List<String>,
        expiry = (claims[KEY_EXP] as Date).time / 1000,
        issuedAtTime = (claims[KEY_IAT] as Date).time / 1000,
        issuerHost = claims[KEY_ISS] as String,
        principal = claims[KEY_SUB] as String,
        nonce = claims[KEY_NONCE] as? String,
        extIsAnonymous = claims[KEY_IS_ANONYMOUS] as Boolean,
        delegatedIdentity =
            gson
                .toJson(claims[KEY_DELEGATED_IDENTITY] as Map<*, *>)
                .toByteArray(),
    )
}
