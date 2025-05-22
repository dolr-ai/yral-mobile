package com.yral.shared.features.auth.data

import com.yral.shared.core.AppConfigurations.METADATA_BASE_URL
import com.yral.shared.core.AppConfigurations.OAUTH_BASE_URL
import com.yral.shared.features.auth.data.models.TokenResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json

class AuthDataSourceImpl(
    private val client: HttpClient,
    private val json: Json,
) : AuthDataSource {
    override suspend fun obtainAnonymousIdentity(): TokenResponseDto {
        val formData =
            listOf(
                "grant_type" to GRANT_TYPE_CLIENT_CREDS,
                "client_id" to CLIENT_ID,
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

    override suspend fun authenticateToken(
        code: String,
        verifier: String,
    ): TokenResponseDto {
        val formData =
            listOf(
                "grant_type" to GRANT_TYPE_AUTHORIZATION,
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

    override suspend fun refreshToken(token: String): TokenResponseDto {
        val formData =
            listOf(
                "grant_type" to GRANT_TYPE_REFRESH_TOKEN,
                "refresh_token" to token,
                "client_id" to CLIENT_ID,
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

    override suspend fun updateSessionAsRegistered(
        idToken: String,
        canisterId: String,
    ) {
        client
            .post {
                url {
                    host = METADATA_BASE_URL
                    path(UPDATE_SESSION_AS_REGISTERED, canisterId)
                }
                headers.append("authorization", "Bearer $idToken")
            }.bodyAsText()
    }

    companion object {
        const val REDIRECT_URI_SCHEME = "yral"
        const val REDIRECT_URI_HOST = "oauth"
        const val REDIRECT_URI_PATH = "/callback"
        const val REDIRECT_URI = "$REDIRECT_URI_SCHEME://$REDIRECT_URI_HOST$REDIRECT_URI_PATH"
        const val CLIENT_ID = "c89b29de-8366-4e62-9b9e-c29585740acf"
        private const val PATH_AUTHENTICATE_TOKEN = "oauth/token"
        private const val GRANT_TYPE_AUTHORIZATION = "authorization_code"
        private const val GRANT_TYPE_CLIENT_CREDS = "client_credentials"
        private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
        private const val UPDATE_SESSION_AS_REGISTERED = "update_session_as_registered"
    }
}
