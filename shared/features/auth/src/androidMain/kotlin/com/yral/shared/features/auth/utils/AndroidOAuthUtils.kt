package com.yral.shared.features.auth.utils

import android.app.Activity
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jwt.JWTParser
import com.yral.shared.features.auth.di.AuthEnv
import com.yral.shared.features.auth.domain.models.TokenClaims
import io.ktor.http.Url
import io.ktor.http.toURI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date

class AndroidOAuthUtils : OAuthUtils {
    override var callBack: ((OAuthResult) -> Unit)? = null
    override var callbackExpiry: Long = 0L

    companion object {
        private const val CALLBACK_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
    }

    override fun openOAuth(
        context: Any,
        authUrl: Url,
        callBack: (result: OAuthResult) -> Unit,
    ) {
        if (context is Activity) {
            Logger.d("OAuthUtils") { "Starting OAuth flow with URL: $authUrl" }
            this.callBack = callBack
            this.callbackExpiry = System.currentTimeMillis() + CALLBACK_TIMEOUT_MS
            val customTabsIntent =
                CustomTabsIntent
                    .Builder()
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(false)
                    .build()
            // Don't add activity flags that might destroy the calling activity
            // Set the data URI
            val uri = authUrl.toURI().toString().toUri()
            customTabsIntent.launchUrl(context, uri)
        } else {
            callBack(OAuthResult.Error(error = "invalid_context", errorDescription = "Context must be an Activity"))
        }
    }

    override fun invokeCallback(result: OAuthResult) {
        // Only check expiry if there's an active OAuth flow (callbackExpiry > 0)
        if (this.callbackExpiry > 0 && System.currentTimeMillis() > this.callbackExpiry) {
            Logger.e("OAuthUtils") { "OAuth callback expired - invoking with timeouted result" }
            callBack?.invoke(OAuthResult.TimedOut)
            cleanup()
            return
        }
        callBack?.invoke(result)
        cleanup()
    }

    override fun cleanup() {
        Logger.d("OAuthUtils") { "Cleaning up OAuth callback and expiry" }
        callBack = null
        callbackExpiry = 0L
    }
}

class AndroidOAuthUtilsHelper(
    authEnv: AuthEnv,
) : OAuthUtilsHelper {
    private val redirectUri: AuthEnv.RedirectUri = authEnv.redirectUri

    override fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(CODE_VERIFIER_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    override fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    override fun generateState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(CODE_VERIFIER_LENGTH)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    override fun parseOAuthToken(token: String): TokenClaims {
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
                    .toJson(claims[KEY_DELEGATED_IDENTITY] as Map<*, *>?)
                    .toByteArray(),
            email = claims[KEY_EMAIL] as? String,
        )
    }

    override fun mapUriToOAuthResult(uri: String): OAuthResult? {
        val uriObj = uri.toUri()
        if (uriObj.scheme == redirectUri.scheme &&
            uriObj.host == redirectUri.host &&
            uriObj.path == redirectUri.path
        ) {
            Logger.d("MainActivity") { "Processing OAuth redirect: $uri" }
            val code = uriObj.getQueryParameter("code")
            val state = uriObj.getQueryParameter("state")
            val error = uriObj.getQueryParameter("error")
            val errorDescription = uriObj.getQueryParameter("error_description")
            val result =
                when {
                    !error.isNullOrBlank() -> {
                        OAuthResult.Error(error = error, errorDescription = errorDescription)
                    }
                    !code.isNullOrBlank() && !state.isNullOrBlank() -> {
                        OAuthResult.Success(code = code, state = state)
                    }
                    else -> {
                        OAuthResult.Error(error = "unknown_error", errorDescription = "Missing required parameters")
                    }
                }
            return result
        } else {
            return null
        }
    }
}
