package com.yral.shared.iap.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Purchase(
    val productId: ProductId?,
    val purchaseToken: String? = null,
    val receipt: String? = null,
    val purchaseTime: Long,
    val state: PurchaseState,
    val expirationDate: Long? = null,
    val isAutoRenewing: Boolean? = null,
    val subscriptionStatus: SubscriptionStatus? = null,
    val accountIdentifier: String? = null,
) {
    fun isActiveSubscription(): Boolean =
        when (subscriptionStatus) {
            SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED -> true
            else -> false
        }
}
