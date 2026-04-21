package com.yral.shared.features.auth.data

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.yral.shared.core.AppConfigurations.METADATA_BASE_URL
import com.yral.shared.core.AppConfigurations.OAUTH_BASE_URL
import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.data.removedFirebaseCloudFunctionsException
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
import com.yral.shared.http.HTTPEventListener
import com.yral.shared.http.httpDelete
import com.yral.shared.http.httpPost
import com.yral.shared.http.httpPostWithStringResponse
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.utils.SignedDelegationPayload
import com.yral.shared.rust.service.utils.delegatedIdentityWireToJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private typealias AuthRequestBlock = HttpRequestBuilder.(host: String) -> Unit

class AuthDataSourceImpl(
    private val client: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val authEnv: AuthEnv,
    private val authHostResolver: SessionAuthHostResolver,
    private val httpEventListener: HTTPEventListener,
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
            authPostWithStringResponse { host ->
                url {
                    this.host = host
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
            authPostWithStringResponse { host ->
                url {
                    this.host = host
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
            authPostWithStringResponse { host ->
                url {
                    this.host = host
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
    ): ExchangePrincipalResponseDto = throw removedFirebaseCloudFunctionsException("exchangePrincipalId")

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
                .registerDevice(identity, token, authEnv.notificationEnvironment)
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
                .unregisterDevice(identity, token, authEnv.notificationEnvironment)
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
            authPostResponse { host ->
                expectSuccess = false
                url {
                    this.host = host
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
            authPostResponse { host ->
                expectSuccess = false
                url {
                    this.host = host
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
        delegations: List<SignedDelegationPayload>?,
    ): CreateAiAccountResponseDto =
        authPost<CreateAiAccountResponseDto> { host ->
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
                this.host = host
                // Endpoint: POST https://auth.yral.com/api/create_ai_account
                path(PATH_CREATE_AI_ACCOUNT)
            }
            setBody(payload)
        }.also { response: CreateAiAccountResponseDto ->
            logger.d { "create_ai_account response=${json.encodeToString(response)}" }
        }

    private suspend inline fun <reified T> authPost(crossinline block: AuthRequestBlock): T =
        executeAuthRequestWithFallback { host ->
            httpPost(
                httpClient = client,
                json = json,
            ) {
                block(host)
            }
        }

    private suspend fun authPostResponse(block: AuthRequestBlock): HttpResponse =
        executeAuthRequestWithFallback { host ->
            client.post {
                block(host)
            }
        }

    private suspend fun authPostWithStringResponse(block: AuthRequestBlock): String =
        executeAuthRequestWithFallback { host ->
            httpPostWithStringResponse(client) {
                block(host)
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> executeAuthRequestWithFallback(request: suspend (host: String) -> T): T {
        val initialHost = authHostResolver.currentHost()
        return runAuthRequestForHost(
            host = initialHost,
            allowFallback = initialHost == OAUTH_BASE_URL,
            request = request,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> runAuthRequestForHost(
        host: String,
        allowFallback: Boolean,
        request: suspend (host: String) -> T,
    ): T {
        return try {
            request(host)
        } catch (exception: Exception) {
            if (exception.isDnsResolutionFailure()) {
                reportDnsFailureIfNeeded(host, exception)
                if (allowFallback) {
                    val fallbackHost = authHostResolver.activateFallback(host)
                    return runAuthRequestForHost(
                        host = fallbackHost,
                        allowFallback = false,
                        request = request,
                    )
                }
            }
            throw exception
        }
    }

    private fun reportDnsFailureIfNeeded(
        host: String,
        exception: Throwable,
    ) {
        if (!platformReportsDnsLookupFailure()) {
            httpEventListener.logException(exception.toDnsLookupException(host))
        }
    }

    companion object {
        private const val PATH_AUTHENTICATE_TOKEN = "oauth/token"
        private const val GRANT_TYPE_AUTHORIZATION = "authorization_code"
        private const val GRANT_TYPE_CLIENT_CREDS = "client_credentials"
        private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
        private const val UPDATE_SESSION_AS_REGISTERED = "/v2/update_session_as_registered"
        private const val DELETE_ACCOUNT = "api/v1/user"
        private const val PATH_PHONE_AUTH_LOGIN = "api/phone_auth_login"
        private const val PATH_VERIFY_PHONE_AUTH = "api/verify_phone_auth"
        private const val PATH_CREATE_AI_ACCOUNT = "api/create_ai_account"
    }
}
