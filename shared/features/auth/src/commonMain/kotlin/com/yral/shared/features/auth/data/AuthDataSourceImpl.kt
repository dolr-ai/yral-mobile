package com.yral.shared.features.auth.data

import com.yral.shared.features.auth.data.models.TokenResponseDto
import com.yral.shared.features.auth.utils.createAuthPayload
import com.yral.shared.http.CookieType
import com.yral.shared.http.httpPost
import com.yral.shared.http.httpPostWithBytesResponse
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class AuthDataSourceImpl(
    private val client: HttpClient,
    private val json: Json,
) : AuthDataSource {
    override suspend fun setAnonymousIdentityCookie() {
        val payload = createAuthPayload()
        httpPost<String?>(
            httpClient = client,
            json = json,
        ) {
            url {
                host = ANONYMOUS_AUTH_BASE_URL
                path(PATH_SET_ANONYMOUS_IDENTITY_COOKIE)
            }
            setBody(payload)
        }
    }

    override suspend fun extractIdentity(cookie: Cookie): ByteArray {
        val payload = JsonObject(mapOf()).toString().toByteArray()
        return httpPostWithBytesResponse(client) {
            url {
                host = ANONYMOUS_AUTH_BASE_URL
                path(PATH_EXTRACT_IDENTITY)
            }
            headers {
                "Cookie" to "${cookie.name}=${cookie.value}"
            }
            setBody(payload)
        }
    }

    override suspend fun authenticateToken(
        code: String,
        verifier: String,
    ): TokenResponseDto {
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
                        path(PATH_AUTHENTICATE_TOKEN)
                    }
                    setBody(formData)
                    contentType(ContentType.Application.FormUrlEncoded)
                }.bodyAsText()
        return json.decodeFromString<TokenResponseDto>(response)
    }

    override suspend fun getAnonymousIdentityCookie(): Cookie? =
        client
            .cookies("https://$ANONYMOUS_AUTH_BASE_URL")
            .firstOrNull { it.name == CookieType.USER_IDENTITY.value }

    companion object {
        private const val ANONYMOUS_AUTH_BASE_URL = "yral.com"
        const val OAUTH_BASE_URL = "yral-auth-v2.fly.dev"
        const val REDIRECT_URI = "yral://oauth/callback"
        const val CLIENT_ID = "c89b29de-8366-4e62-9b9e-c29585740acf"

        private const val PATH_SET_ANONYMOUS_IDENTITY_COOKIE = "api/set_anonymous_identity_cookie"
        private const val PATH_EXTRACT_IDENTITY = "api/extract_identity"
        private const val PATH_AUTHENTICATE_TOKEN = "oauth/token"
    }
}
