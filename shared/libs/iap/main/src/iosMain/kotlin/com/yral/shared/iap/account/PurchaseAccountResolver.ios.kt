package com.yral.shared.iap.account

import co.touchlab.kermit.Logger
import com.yral.shared.iap.core.IAPError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AppleAppAccountTokenRequest(
    @SerialName("user_id") val userId: String,
)

@Serializable
private data class AppleAppAccountTokenData(
    @SerialName("app_account_token") val appAccountToken: String,
)

@Serializable
private data class AppleAppAccountTokenResponse(
    val success: Boolean,
    val msg: String? = null,
    val error: String? = null,
    val data: AppleAppAccountTokenData? = null,
)

internal actual fun createPurchaseAccountResolver(
    httpClient: HttpClient,
    json: Json,
    billingBaseUrl: String,
): PurchaseAccountResolver = IOSPurchaseAccountResolver(httpClient, json, billingBaseUrl)

private class IOSPurchaseAccountResolver(
    private val httpClient: HttpClient,
    private val json: Json,
    private val billingBaseUrl: String,
) : PurchaseAccountResolver {
    override suspend fun resolve(userId: String): String {
        val response =
            httpClient.post {
                expectSuccess = false
                url {
                    host = billingBaseUrl
                    path(APPLE_APP_ACCOUNT_TOKEN_ENDPOINT)
                }
                setBody(AppleAppAccountTokenRequest(userId))
            }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            Logger.w(TAG) { "Apple app account token request failed: status=${response.status}, body=$body" }
            throw IAPError.VerificationFailed(
                "apple_app_account_token",
                Exception("Failed to get Apple app account token: ${response.status.value}"),
            )
        }

        val decoded = json.decodeFromString<AppleAppAccountTokenResponse>(body)
        val token = decoded.data?.appAccountToken
        if (!decoded.success || token.isNullOrBlank()) {
            throw IAPError.VerificationFailed(
                "apple_app_account_token",
                Exception(decoded.error ?: decoded.msg ?: "Apple app account token missing"),
            )
        }

        return token
    }

    companion object {
        private const val TAG = "SubscriptionXM"
        private const val APPLE_APP_ACCOUNT_TOKEN_ENDPOINT = "apple/app-account-token"
    }
}
