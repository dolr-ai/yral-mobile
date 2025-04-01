package com.yral.shared.rust.auth

import com.yral.shared.rust.http.BaseURL
import com.yral.shared.rust.http.HttpClientFactory.client
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.authenticateWithNetwork
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Cookie
import io.ktor.http.headers
import kotlinx.serialization.json.JsonObject


class DefaultAuthClient : AuthClient {
    override var identity: ByteArray? = null
    override var canisterPrincipal: Principal? = null
    override var userPrincipal: Principal? = null

    override suspend fun initialize() {
        try {
            fetchAndSetAuthCookie()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun refreshAuthIfNeeded(cookie: HttpCookies) {
        TODO("Not yet implemented")
    }

    override suspend fun generateNewDelegatedIdentity(): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun generateNewDelegatedIdentityWireOneHour(): ByteArray {
        TODO("Not yet implemented")
    }

    private suspend fun fetchAndSetAuthCookie() {
        val setAnonymousIdentityCookiePath = "api/set_anonymous_identity_cookie"
        val payload = createAuthPayload()
        client.post(setAnonymousIdentityCookiePath) {
            setBody(payload)
        }
        val newCookie = client.cookies("https://${BaseURL}").firstOrNull { it.name == "user-identity" }
        println("xxxxx newCookie: $newCookie")
        newCookie?.let {
            extractIdentity(it)
        }
    }

    private suspend fun extractIdentity(cookie: Cookie) {
        val extractIdentityPath = "api/extract_identity"
        val payload = JsonObject(mapOf()).toString().toByteArray()
        val result = client.post(extractIdentityPath) {
            headers {
                "Cookie" to "${cookie.name}=${cookie.value}"
            }
            setBody(payload)
        }
        println("xxxxx result: ${result.bodyAsBytes()}")
        if (result.bodyAsBytes().isNotEmpty()) {
            handleExtractIdentityResponse(result.bodyAsBytes())
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
