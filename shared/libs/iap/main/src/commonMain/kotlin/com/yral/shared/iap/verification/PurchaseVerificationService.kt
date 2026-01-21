package com.yral.shared.iap.verification

import co.touchlab.kermit.Logger
import com.yral.shared.core.AppConfigurations
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.core.util.handleIAPOperation
import com.yral.shared.iap.utils.PackageNameProvider
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal expect fun getVerifierEndPoint(): String

@Serializable
internal data class VerifyPurchaseRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("product_id") val productId: String,
    @SerialName("purchase_token") val purchaseToken: String,
)

internal class PurchaseVerificationService(
    private val httpClient: HttpClient,
    private val preferences: Preferences,
) {
    companion object {
        private const val TAG = "PurchaseVerificationService"
    }

    @Suppress("LongMethod", "ThrowsCount")
    suspend fun verifyPurchase(
        purchase: Purchase,
        userId: String,
    ): Result<Boolean> =
        handleIAPOperation {
            val productId = purchase.productId?.productId ?: return@handleIAPOperation false
            val purchaseToken = purchase.purchaseToken
            val idToken = preferences.getString(PrefKeys.ID_TOKEN.name)

            if (purchaseToken == null || idToken == null) {
                if (purchaseToken == null) {
                    Logger.w(TAG) { "Purchase token is null for product ${purchase.productId}" }
                }
                if (idToken == null) {
                    Logger.w(TAG) { "ID token not found - cannot verify purchase" }
                }
                throw IAPError.UnknownError(
                    Exception(
                        "Missing required tokens for verification. " +
                            "Purchase token: ${if (purchaseToken == null) "null" else "present"}, " +
                            "ID token: ${if (idToken == null) "null" else "present"}",
                    ),
                )
            }

            val response =
                try {
                    httpClient.post {
                        url {
                            host = AppConfigurations.BILLING_BASE_URL
                            path(getVerifierEndPoint())
                        }
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $idToken")
                        }
                        setBody(
                            VerifyPurchaseRequest(
                                userId = userId,
                                packageName = PackageNameProvider.getPackageName(),
                                productId = productId,
                                purchaseToken = purchaseToken,
                            ),
                        )
                    }
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Logger.e(TAG, e) { "Network error during purchase verification for product ${purchase.productId}" }
                    throw IAPError.NetworkError(
                        Exception("Network error during purchase verification", e),
                    )
                }

            val isSuccess = response.status.isSuccess()
            if (!isSuccess) {
                Logger.w(TAG) {
                    "Purchase verification failed for product $productId. " +
                        "HTTP status: ${response.status.value}"
                }
                throw IAPError.VerificationFailed(
                    productId,
                    Exception("Backend verification failed with HTTP status: ${response.status.value}"),
                )
            }

            true
        }
}
