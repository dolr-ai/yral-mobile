package com.yral.shared.features.auth.data

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.yral.shared.core.AppConfigurations.METADATA_BASE_URL
import com.yral.shared.core.AppConfigurations.OAUTH_BASE_URL
import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.auth.data.models.DeleteAccountRequestDto
import com.yral.shared.features.auth.data.models.ExchangePrincipalResponseDto
import com.yral.shared.features.auth.data.models.TokenResponseDto
import com.yral.shared.features.auth.di.AuthEnv
import com.yral.shared.firebaseStore.cloudFunctionUrl
import com.yral.shared.firebaseStore.firebaseAppCheckToken
import com.yral.shared.http.httpDelete
import com.yral.shared.http.httpPost
import com.yral.shared.http.httpPostWithStringResponse
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.utils.delegatedIdentityWireToJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json

class AuthDataSourceImpl(
    private val client: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val authEnv: AuthEnv,
) : AuthDataSource {
    override suspend fun obtainAnonymousIdentity(): TokenResponseDto {
        val formData =
            listOf(
                "grant_type" to GRANT_TYPE_CLIENT_CREDS,
                "client_id" to authEnv.clientId,
            ).joinToString("&") { (key, value) ->
                "$key=$value"
            }
        val response =
            httpPostWithStringResponse(client) {
                url {
                    host = OAUTH_BASE_URL
                    path(PATH_AUTHENTICATE_TOKEN)
                }
                setBody(formData)
                contentType(ContentType.Application.FormUrlEncoded)
            }
        return json.decodeFromString<TokenResponseDto>(response)
    }

    override suspend fun authenticateToken(
        code: String,
        verifier: String,
    ): TokenResponseDto {
        val formData =
            listOf(
                "grant_type" to GRANT_TYPE_AUTHORIZATION,
                "client_id" to authEnv.clientId,
                "code" to code,
                "code_verifier" to verifier,
                "redirect_uri" to authEnv.redirectUri.uriString,
            ).joinToString("&") { (key, value) ->
                "$key=$value"
            }
        val response =
            httpPostWithStringResponse(client) {
                url {
                    host = OAUTH_BASE_URL
                    path(PATH_AUTHENTICATE_TOKEN)
                }
                setBody(formData)
                contentType(ContentType.Application.FormUrlEncoded)
            }
        return json.decodeFromString<TokenResponseDto>(response)
    }

    override suspend fun refreshToken(token: String): TokenResponseDto {
        val formData =
            listOf(
                "grant_type" to GRANT_TYPE_REFRESH_TOKEN,
                "refresh_token" to token,
                "client_id" to authEnv.clientId,
            ).joinToString("&") { (key, value) ->
                "$key=$value"
            }
        val response =
            httpPostWithStringResponse(client) {
                url {
                    host = OAUTH_BASE_URL
                    path(PATH_AUTHENTICATE_TOKEN)
                }
                setBody(formData)
                contentType(ContentType.Application.FormUrlEncoded)
            }
        return json.decodeFromString<TokenResponseDto>(response)
    }

    override suspend fun updateSessionAsRegistered(
        idToken: String,
        canisterId: String,
        userPrincipal: String,
    ) {
        httpPostWithStringResponse(client) {
            expectSuccess = false
            url {
                host = METADATA_BASE_URL
                path(UPDATE_SESSION_AS_REGISTERED)
            }
            headers.append("authorization", "Bearer $idToken")
            setBody(
                mapOf(
                    "user_canister" to canisterId,
                    "user_principal" to userPrincipal,
                ),
            )
        }
    }

    override suspend fun exchangePrincipalId(
        idToken: String,
        principalId: String,
    ): ExchangePrincipalResponseDto =
        httpPost(
            httpClient = client,
            json = json,
        ) {
            url {
                host = cloudFunctionUrl()
                path(EXCHANGE_PRINCIPAL_PATH)
            }
            val appCheckToken = firebaseAppCheckToken()
            headers {
                append(HttpHeaders.Authorization, "Bearer $idToken")
                append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
            }
            setBody(
                mapOf(
                    "data" to
                        mapOf(
                            "principal_id" to principalId,
                        ),
                ),
            )
        }

    override suspend fun deleteAccount(): String {
        val identityWire = preferences.getBytes(PrefKeys.IDENTITY.name)
        return identityWire?.let {
            val identityWireJson = delegatedIdentityWireToJson(identityWire)
            val delegatedIdentity =
                json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)
            val params =
                DeleteAccountRequestDto(
                    delegatedIdentity = delegatedIdentity,
                )
            httpDelete(client) {
                url {
                    host = OFF_CHAIN_BASE_URL
                    path(DELETE_ACCOUNT)
                }
                setBody(params)
            }
        } ?: throw YralException("Identity not found while deleting account")
    }

    override suspend fun registerForNotifications(token: String) {
        val identityWire = preferences.getBytes(PrefKeys.IDENTITY.name)
        identityWire?.let { identity ->
            Logger.d("AuthDataSource") { "registerForNotifications: token $token" }
            HelperService
                .registerDevice(identity, token)
                .onFailure { error ->
                    Logger.e("AuthDataSource") { "Failed to register device: ${error.message}" }
                    throw YralException("Failed to register device: ${error.message}")
                }
        } ?: throw YralException("Identity not found while registering for notifications")
    }

    override suspend fun deregisterForNotifications(token: String) {
        val identityWire = preferences.getBytes(PrefKeys.IDENTITY.name)
        identityWire?.let { identity ->
            HelperService
                .unregisterDevice(identity, token)
                .onFailure { error ->
                    Logger.e("AuthDataSource") { "Failed to unregister device: ${error.message}" }
                    throw YralException("Failed to unregister device: ${error.message}")
                }
        } ?: throw YralException("Identity not found while deregistering for notifications")
    }

    companion object {
        private const val PATH_AUTHENTICATE_TOKEN = "oauth/token"
        private const val GRANT_TYPE_AUTHORIZATION = "authorization_code"
        private const val GRANT_TYPE_CLIENT_CREDS = "client_credentials"
        private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
        private const val UPDATE_SESSION_AS_REGISTERED = "/v2/update_session_as_registered"
        private const val EXCHANGE_PRINCIPAL_PATH = "exchange_principal_id"
        private const val HEADER_X_FIREBASE_APPCHECK = "X-Firebase-AppCheck"
        private const val DELETE_ACCOUNT = "api/v1/user"
    }
}
