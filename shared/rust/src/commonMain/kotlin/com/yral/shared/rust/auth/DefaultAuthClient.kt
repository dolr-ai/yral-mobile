package com.yral.shared.rust.auth

import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.PrefUtils
import com.yral.shared.rust.http.BaseURL
import com.yral.shared.rust.http.CookieType
import com.yral.shared.rust.http.HttpClientFactory.client
import com.yral.shared.rust.http.maxAgeOrExpires
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Cookie
import io.ktor.http.headers
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject


class DefaultAuthClient : AuthClient {
    override var identity: ByteArray? = null
    override var canisterPrincipal: Principal? = null
    override var userPrincipal: Principal? = null

    override suspend fun initialize() {
        try {
            refreshAuthIfNeeded()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun refreshAuthIfNeeded() {
        val cookie = client.cookies("https://$BaseURL").firstOrNull { it.name == CookieType.USER_IDENTITY.value }
        cookie?.let {
            if ((it.maxAgeOrExpires(Clock.System.now().toEpochMilliseconds()) ?: 0) > Clock.System.now().toEpochMilliseconds()) {
                val storedData = PrefUtils().getByteArray(PrefKeys.IDENTITY_DATA)
                storedData?.let { data -> handleExtractIdentityResponse(data) } ?: extractIdentity(it)
            } else {
                fetchAndSetAuthCookie()
            }
        } ?: fetchAndSetAuthCookie()
    }

    override suspend fun generateNewDelegatedIdentity(): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun generateNewDelegatedIdentityWireOneHour(): ByteArray {
        TODO("Not yet implemented")
    }

    private suspend fun fetchAndSetAuthCookie() {
        PrefUtils().remove(CookieType.USER_IDENTITY.value)
        PrefUtils().remove(PrefKeys.IDENTITY_DATA)
        val setAnonymousIdentityCookiePath = "api/set_anonymous_identity_cookie"
        val payload = createAuthPayload()
        client.post(setAnonymousIdentityCookiePath) {
            setBody(payload)
        }
        refreshAuthIfNeeded()
    }

    private suspend fun extractIdentity(cookie: Cookie) {
        val extractIdentityPath = "api/extract_identity"
        val payload = JsonObject(mapOf()).toString().toByteArray()
        val result = client.post(extractIdentityPath) {
            headers {
                "Cookie" to "${cookie.name}=${cookie.value}"
            }
            setBody(payload)
        }.bodyAsBytes()
        if (result.isNotEmpty()) {
            handleExtractIdentityResponse(result)
            PrefUtils().putByteArray(PrefKeys.IDENTITY_DATA, result)
        }
    }

    private suspend fun handleExtractIdentityResponse(data: ByteArray) {
        identity = data
        val canisterWrapper = authenticateWithNetwork(data, null)
        canisterPrincipal = canisterWrapper.getCanisterPrincipal()
        userPrincipal = canisterWrapper.getUserPrincipal()
        println("xxxxx canisterPrincipal: $canisterPrincipal")
        println("xxxxx userPrincipal: $userPrincipal")
    }
}
