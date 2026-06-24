package com.yral.shared.iap.verification

import co.touchlab.kermit.Logger
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.core.util.handleIAPOperation
import com.yral.shared.iap.utils.PackageNameProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal expect fun getVerifierEndPoint(): String
internal expect fun supportsAppleAppAccountToken(): Boolean

@Serializable
internal data class VerifyPurchaseRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("product_id") val productId: String,
    @SerialName("purchase_token") val purchaseToken: String,
)

@Serializable
internal data class AppleAppAccountTokenRequest(
    @SerialName("user_id") val userId: String,
)

@Serializable
internal data class AppleAppAccountTokenResponseData(
    @SerialName("app_account_token") val appAccountToken: String,
)

@Serializable
internal data class AppleAppAccountTokenResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("msg") val msg: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("data") val data: AppleAppAccountTokenResponseData? = null,
)

@Serializable
internal data class ErrorResponse(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("msg") val msg: String? = null,
    @SerialName("error") val error: String? = null,
)

internal class PurchaseVerificationService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val billingBaseUrl: String,
) {
    companion object {
        private const val TAG = "SubscriptionXM"
        private const val APPLE_APP_ACCOUNT_TOKEN_ENDPOINT = "apple/app-account-token"
    }

    @Suppress("LongMethod", "ThrowsCount")
    suspend fun verifyPurchase(
        purchase: Purchase,
        userId: String,
    ): Result<Boolean> =
        handleIAPOperation {
            val productId =
                purchase.productId?.productId
                    ?: throw IAPError.VerificationFailed("unknown", Exception("Missing product ID"))
            val purchaseToken =
                purchase.purchaseToken
                    ?: throw IAPError.VerificationFailed(productId, Exception("Missing purchase token"))
            val request =
                VerifyPurchaseRequest(
                    userId = userId,
                    packageName = PackageNameProvider.getPackageName(),
                    productId = productId,
                    purchaseToken = purchaseToken,
                )
            Logger.d(TAG) { "Verifying purchase $request" }
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        applyBillingBaseUrl(billingBaseUrl)
                        path(getVerifierEndPoint())
                    }
                    setBody(request)
                }

            if (!response.status.isSuccess()) {
                val errorMessage =
                    try {
                        val errorBody = response.bodyAsText()
                        val errorResponse = json.decodeFromString<ErrorResponse>(errorBody)
                        errorResponse.error ?: errorResponse.msg ?: "Unknown error (status: ${response.status.value})"
                    } catch (
                        @Suppress("TooGenericExceptionCaught")
                        e: Exception,
                    ) {
                        Logger.e(TAG, e) {
                            "Failed to parse error response for product $productId (status: ${response.status.value})"
                        }
                        "Backend verification failed with HTTP status: ${response.status.value}"
                    }

                Logger.w(TAG) { "Verification failed for $productId: $errorMessage" }
                throw IAPError.VerificationFailed(productId, Exception(errorMessage))
            }

            true
        }

    suspend fun getAppleAppAccountToken(userId: String): Result<String> =
        handleIAPOperation {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        applyBillingBaseUrl(billingBaseUrl)
                        path(APPLE_APP_ACCOUNT_TOKEN_ENDPOINT)
                    }
                    setBody(AppleAppAccountTokenRequest(userId = userId))
                }

            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val errorMessage =
                    runCatching {
                        val errorResponse = json.decodeFromString<ErrorResponse>(body)
                        errorResponse.error ?: errorResponse.msg ?: "Unknown error (status: ${response.status.value})"
                    }.getOrElse {
                        "Apple app account token failed with HTTP status: ${response.status.value}"
                    }
                throw IAPError.VerificationFailed(APPLE_APP_ACCOUNT_TOKEN_ENDPOINT, Exception(errorMessage))
            }

            val parsed = json.decodeFromString<AppleAppAccountTokenResponse>(body)
            if (!parsed.success) {
                throw IAPError.VerificationFailed(
                    APPLE_APP_ACCOUNT_TOKEN_ENDPOINT,
                    Exception(parsed.error ?: parsed.msg ?: "Apple app account token request failed"),
                )
            }

            parsed.data?.appAccountToken
                ?: throw IAPError.VerificationFailed(
                    APPLE_APP_ACCOUNT_TOKEN_ENDPOINT,
                    Exception("Missing app_account_token"),
                )
        }

    private fun URLBuilder.applyBillingBaseUrl(baseUrl: String) {
        val normalized = if ("://" in baseUrl) baseUrl else "https://$baseUrl"
        takeFrom(normalized)
    }
}
