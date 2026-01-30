package com.yral.shared.iap.verification

import co.touchlab.kermit.Logger
import com.yral.shared.core.AppConfigurations
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
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal expect fun getVerifierEndPoint(): String

@Serializable
internal data class VerifyPurchaseRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("product_id") val productId: String,
    @SerialName("purchase_token") val purchaseToken: String,
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
) {
    companion object {
        private const val TAG = "SubscriptionXM"
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
                        host = AppConfigurations.BILLING_BASE_URL
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
}
