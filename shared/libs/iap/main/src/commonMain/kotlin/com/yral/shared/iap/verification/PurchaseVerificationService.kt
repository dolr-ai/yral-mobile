package com.yral.shared.iap.verification

import co.touchlab.kermit.Logger
import com.yral.shared.core.AppConfigurations
import com.yral.shared.iap.core.model.Purchase
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
        const val TAG = "PurchaseVerificationService"
        const val GOOGLE_VERIFY_PATH = "google/verify"
    }

    suspend fun verifyPurchase(
        purchase: Purchase,
        userId: String,
    ): Boolean {
        val purchaseToken = purchase.purchaseToken
        val idToken = preferences.getString(PrefKeys.ID_TOKEN.name)

        if (purchaseToken == null || idToken == null) {
            if (purchaseToken == null) {
                Logger.w(TAG) { "Purchase token is null for product ${purchase.productId}" }
            }
            if (idToken == null) {
                Logger.w(TAG) { "ID token not found - cannot verify purchase" }
            }
            return false
        }

        val response =
            httpClient.post {
                url {
                    host = AppConfigurations.BILLING_BASE_URL
                    path(GOOGLE_VERIFY_PATH)
                }
                headers {
                    append(HttpHeaders.Authorization, "Bearer $idToken")
                }
                setBody(
                    VerifyPurchaseRequest(
                        userId = userId,
                        packageName = PackageNameProvider.getPackageName(),
                        productId = purchase.productId,
                        purchaseToken = purchaseToken,
                    ),
                )
            }
        return response.status.isSuccess()
    }
}
