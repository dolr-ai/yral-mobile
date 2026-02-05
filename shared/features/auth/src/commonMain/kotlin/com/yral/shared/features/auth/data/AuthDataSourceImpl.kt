package com.yral.shared.features.auth.data

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.yral.shared.core.AppConfigurations.METADATA_BASE_URL
import com.yral.shared.core.AppConfigurations.OAUTH_BASE_URL
import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.auth.data.models.AuthClientQuery
import com.yral.shared.features.auth.data.models.CreateAiAccountRequestDto
import com.yral.shared.features.auth.data.models.CreateAiAccountResponseDto
import com.yral.shared.features.auth.data.models.DelegationDto
import com.yral.shared.features.auth.data.models.DeleteAccountRequestDto
import com.yral.shared.features.auth.data.models.ExchangePrincipalResponseDto
import com.yral.shared.features.auth.data.models.IngressExpiryDto
import com.yral.shared.features.auth.data.models.PhoneAuthLoginRequestDto
import com.yral.shared.features.auth.data.models.PhoneAuthLoginResponseDto
import com.yral.shared.features.auth.data.models.PhoneAuthVerifyRequestDto
import com.yral.shared.features.auth.data.models.PhoneAuthVerifyResponseDto
import com.yral.shared.features.auth.data.models.SignaturePayloadDto
import com.yral.shared.features.auth.data.models.SignedDelegationDto
import com.yral.shared.features.auth.data.models.TokenResponseDto
import com.yral.shared.features.auth.data.models.VerifyRequestDto
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthDataSourceImpl(
    private val client: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val authEnv: AuthEnv,
) : AuthDataSource {
    private val logger = Logger.withTag("AuthDataSourceImpl")

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

    override suspend fun phoneAuthLogin(
        phoneNumber: String,
        authClientQuery: AuthClientQuery,
    ): PhoneAuthLoginResponseDto {
        val requestBody =
            PhoneAuthLoginRequestDto(
                authClientQuery = authClientQuery,
                phoneNumber = phoneNumber,
            )
        val response =
            client.post {
                expectSuccess = false
                url {
                    host = OAUTH_BASE_URL
                    path(PATH_PHONE_AUTH_LOGIN)
                }
                setBody(requestBody)
                contentType(ContentType.Application.Json)
            }
        return if (response.status == HttpStatusCode.OK) {
            PhoneAuthLoginResponseDto.Success
        } else {
            val apiResponseString = response.bodyAsText()
            json.decodeFromString<PhoneAuthLoginResponseDto.Error>(apiResponseString)
        }
    }

    override suspend fun verifyPhoneAuth(verifyRequest: VerifyRequestDto): PhoneAuthVerifyResponseDto {
        val requestBody = PhoneAuthVerifyRequestDto(verifyRequest = verifyRequest)
        val response =
            client.post {
                expectSuccess = false
                url {
                    host = OAUTH_BASE_URL
                    path(PATH_VERIFY_PHONE_AUTH)
                }
                setBody(requestBody)
                contentType(ContentType.Application.Json)
            }
        val apiResponseString = response.bodyAsText()
        return if (response.status == HttpStatusCode.OK) {
            val responseArray = json.decodeFromString<List<String>>(apiResponseString)
            if (responseArray.size == 2) {
                PhoneAuthVerifyResponseDto.Success(
                    idTokenCode = responseArray[0],
                    redirectUri = responseArray[1],
                )
            } else {
                PhoneAuthVerifyResponseDto.Error(
                    error = "Missing all required keys",
                    errorDescription = apiResponseString,
                )
            }
        } else {
            json.decodeFromString<PhoneAuthVerifyResponseDto.Error>(apiResponseString)
        }
    }

    override suspend fun createAiAccount(
        userPrincipal: String,
        signature: ByteArray,
        publicKey: ByteArray,
        signedMessage: ByteArray,
        ingressExpirySecs: Long,
        ingressExpiryNanos: Int,
        delegations: List<com.yral.shared.rust.service.utils.SignedDelegationPayload>?,
    ): CreateAiAccountResponseDto =
        httpPost<CreateAiAccountResponseDto>(
            httpClient = client,
            json = json,
        ) {
            val payload =
                CreateAiAccountRequestDto(
                    userPrincipal = userPrincipal,
                    signature =
                        SignaturePayloadDto(
                            sig = signature.map { it.toUByte().toInt() },
                            publicKey = publicKey.map { it.toUByte().toInt() },
                            ingressExpiry = IngressExpiryDto(secs = ingressExpirySecs, nanos = ingressExpiryNanos),
                            delegations =
                                delegations?.map { del ->
                                    SignedDelegationDto(
                                        delegation =
                                            DelegationDto(
                                                pubKey = del.delegation.pubkey.map { it.toUByte().toInt() },
                                                expirationNs = del.delegation.expiration,
                                                targets = del.delegation.targets,
                                            ),
                                        signature = del.signature.map { it.toUByte().toInt() },
                                    )
                                },
                            sender = userPrincipal,
                        ),
                )
            logger.d { "create_ai_account request=${json.encodeToString(payload)}" }
            url {
                host = OAUTH_BASE_URL
                // Endpoint: POST https://auth.yral.com/api/create_ai_account
                path("api", "create_ai_account")
            }
            setBody(payload)
        }.also { response: CreateAiAccountResponseDto ->
            logger.d { "create_ai_account response=${json.encodeToString(response)}" }
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
        private const val PATH_PHONE_AUTH_LOGIN = "api/phone_auth_login"
        private const val PATH_VERIFY_PHONE_AUTH = "api/verify_phone_auth"
    }
}
