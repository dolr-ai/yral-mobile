package com.yral.shared.iap.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class Purchase(
    val productId: String,
    val purchaseToken: String? = null,
    val receipt: String? = null,
    val purchaseTime: Long,
    val state: PurchaseState,
    val expirationDate: Long? = null,
    val isAutoRenewing: Boolean? = null,
    val subscriptionStatus: SubscriptionStatus? = null,
    val accountIdentifier: String? = null,
) {
    @OptIn(ExperimentalTime::class)
    @Suppress("ReturnCount")
    fun isActiveSubscription(): Boolean {
        val status = subscriptionStatus ?: return false
        if (status == SubscriptionStatus.UNKNOWN) return false

        expirationDate?.let { expiry ->
            val currentTime = Clock.System.now().toEpochMilliseconds()
            if (expiry <= currentTime) {
                return false
            }
        }

        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.CANCELLED
    }
}
