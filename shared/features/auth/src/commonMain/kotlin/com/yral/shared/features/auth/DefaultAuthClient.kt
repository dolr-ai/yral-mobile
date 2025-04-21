package com.yral.shared.features.auth

import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.Event
import com.yral.shared.analytics.main.FeatureEvents
import com.yral.shared.analytics.main.Features
import com.yral.shared.http.CookieType
import com.yral.shared.http.httpPost
import com.yral.shared.http.httpPostWithBytesResponse
import com.yral.shared.http.maxAgeOrExpires
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.setBody
import io.ktor.http.Cookie
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class DefaultAuthClient(
    private val analyticsManager: AnalyticsManager,
    private val preferences: Preferences,
    private val client: HttpClient,
    private val json: Json,
) : AuthClient {
    override var identity: ByteArray? = null
    override var canisterPrincipal: Principal? = null
    override var userPrincipal: Principal? = null

    override suspend fun initialize() {
        refreshAuthIfNeeded()
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
        identity = data
        val canisterWrapper = authenticateWithNetwork(data, null)
        canisterPrincipal = canisterWrapper.getCanisterPrincipal()
        userPrincipal = canisterWrapper.getUserPrincipal()
        analyticsManager.trackEvent(
            event =
                Event(
                    featureName = Features.AUTH.name.lowercase(),
                    name = FeatureEvents.AUTH_SUCCESSFUL.name.lowercase(),
                ),
        )
    }

    override suspend fun generateNewDelegatedIdentity(): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun generateNewDelegatedIdentityWireOneHour(): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
        private const val BASE_URL = "yral.com"
    }
}
