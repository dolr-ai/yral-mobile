package com.yral.shared.features.auth

import android.net.Uri
import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.Event
import com.yral.shared.analytics.main.FeatureEvents
import com.yral.shared.analytics.main.Features
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.http.CookieType
import com.yral.shared.http.httpPost
import com.yral.shared.http.httpPostWithBytesResponse
import com.yral.shared.http.maxAgeOrExpires
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import com.yral.shared.uniffi.generated.yralAuthLoginHint
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class DefaultAuthClient(
    private val sessionManager: SessionManager,
    private val analyticsManager: AnalyticsManager,
    private val preferences: Preferences,
    private val client: HttpClient,
    private val json: Json,
    private val platformResourcesFactory: PlatformResourcesFactory,
    appDispatchers: AppDispatchers,
) : AuthClient {
    private var verifier: String = ""
    private var currentState: String? = null
    private var coroutineScope = CoroutineScope(appDispatchers.io)

    override suspend fun initialize() {
        checkSocialSignIn()
    }

    private suspend fun checkSocialSignIn() {
        if (preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) == true) {
            preferences.getBytes(PrefKeys.IDENTITY_DATA.name)?.let {
                handleExtractIdentityResponse(it)
            } ?: refreshAuthIfNeeded()
        } else {
            refreshAuthIfNeeded()
        }
    }

    override suspend fun refreshAuthIfNeeded() {
        val cookie =
            client
                .cookies("https://${BASE_URL}")
                .firstOrNull { it.name == CookieType.USER_IDENTITY.value }
        cookie?.let {
            if ((it.maxAgeOrExpires(Clock.System.now().toEpochMilliseconds()) ?: 0) >
                Clock.System.now().toEpochMilliseconds()
            ) {
                val storedData = preferences.getBytes(PrefKeys.IDENTITY_DATA.name)
                storedData?.let { data -> handleExtractIdentityResponse(data) } ?: extractIdentity(
                    it,
                )
            } else {
                fetchAndSetAuthCookie()
            }
        } ?: fetchAndSetAuthCookie()
    }

    private suspend fun fetchAndSetAuthCookie() {
        val setCookiePath = "api/set_anonymous_identity_cookie"
        preferences.remove(CookieType.USER_IDENTITY.value)
        preferences.remove(PrefKeys.IDENTITY_DATA.name)
        val payload = createAuthPayload()
        httpPost<String?>(
            httpClient = client,
            json = json,
        ) {
            url {
                host = BASE_URL
                path(setCookiePath)
            }
            setBody(payload)
        }
        refreshAuthIfNeeded()
    }

    private suspend fun extractIdentity(cookie: Cookie) {
        val extractIdentityPath = "api/extract_identity"
        val payload = JsonObject(mapOf()).toString().toByteArray()
        val result =
            httpPostWithBytesResponse(client) {
                url {
                    host = BASE_URL
                    path(extractIdentityPath)
                }
                headers {
                    "Cookie" to "${cookie.name}=${cookie.value}"
                }
                setBody(payload)
            }
        if (result.isNotEmpty()) {
            handleExtractIdentityResponse(result)
            preferences.putBytes(PrefKeys.IDENTITY_DATA.name, result)
        }
    }

    private suspend fun handleExtractIdentityResponse(data: ByteArray) {
        val canisterWrapper = authenticateWithNetwork(data, null)
        sessionManager.updateState(
            SessionState.SignedIn(
                session =
                    Session(
                        identity = data,
                        canisterPrincipal = canisterWrapper.getCanisterPrincipal(),
                        userPrincipal = canisterWrapper.getUserPrincipal(),
                    ),
            ),
        )
        analyticsManager.trackEvent(
            event =
                Event(
                    featureName = Features.AUTH.name.lowercase(),
                    name = FeatureEvents.AUTH_SUCCESSFUL.name.lowercase(),
                ),
        )
    }

    override suspend fun signInWithSocial(provider: SocialProvider) {
        verifier = generateCodeVerifier()
        currentState = generateState()
        initiateOAuthFlow(provider)
    }

    private fun initiateOAuthFlow(provider: SocialProvider) {
        sessionManager.getIdentity()?.let { identity ->
            val codeChallenge = generateCodeChallenge(verifier)
            val authUri =
                Uri
                    .Builder()
                    .scheme("https")
                    .authority(OAUTH_BASE_URL)
                    .path("/oauth/auth")
                    .appendQueryParameter("provider", provider.value)
                    .appendQueryParameter("client_id", CLIENT_ID)
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("response_mode", "query")
                    .appendQueryParameter("redirect_uri", REDIRECT_URI)
                    .appendQueryParameter("scope", "openid")
                    .appendQueryParameter("code_challenge", codeChallenge)
                    .appendQueryParameter("code_challenge_method", "S256")
                    .appendQueryParameter("login_hint", yralAuthLoginHint(identity))
                    .appendQueryParameter("state", currentState)
                    .build()

            openOAuth(
                platformResourcesFactory = platformResourcesFactory,
                authUri = authUri,
            )
        }
    }

    override fun handleOAuthCallback(
        code: String,
        state: String,
    ) {
        if (state != currentState) {
            throw SecurityException("Invalid state parameter - possible CSRF attack")
        }
        coroutineScope.launch {
            authenticate(code)
        }
    }

    private suspend fun authenticate(code: String) {
        val tokePath = "oauth/token"
        val formData =
            listOf(
                "grant_type" to "authorization_code",
                "client_id" to CLIENT_ID,
                "code" to code,
                "code_verifier" to verifier,
                "redirect_uri" to REDIRECT_URI,
            ).joinToString("&") { (key, value) ->
                "$key=$value"
            }

        val response =
            client
                .post {
                    url {
                        host = OAUTH_BASE_URL
                        path(tokePath)
                    }
                    setBody(formData)
                    contentType(ContentType.Application.FormUrlEncoded)
                }.bodyAsText()
        val tokenResponse = json.decodeFromString<TokenResponse>(response)
        val identity = parseAccessTokenForIdentity(tokenResponse.accessToken)
        preferences.putString(PrefKeys.REFRESH_TOKEN.name, tokenResponse.refreshToken)
        preferences.putBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name, true)
        preferences.remove(CookieType.USER_IDENTITY.value)
        handleExtractIdentityResponse(identity)
    }

    companion object {
        private const val BASE_URL = "yral.com"
        private const val OAUTH_BASE_URL = "yral-auth-v2.fly.dev"
        private const val REDIRECT_URI = "yral://oauth/callback"
        private const val CLIENT_ID = "c89b29de-8366-4e62-9b9e-c29585740acf"
    }
}
