package com.yral.shared.iap.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Purchase model with subscription metadata.
 *
 * Documentation: `shared/libs/iap/docs/IAP_README.md`
 */
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
    /** Returns true if subscription is active and user has access. */
    @OptIn(ExperimentalTime::class)
    @Suppress("ReturnCount")
    fun isActiveSubscription(): Boolean {
        val status = subscriptionStatus ?: return false
        if (status == SubscriptionStatus.UNKNOWN) return false

        // Check expiration date if available
        expirationDate?.let { expiry ->
            val currentTime = Clock.System.now().toEpochMilliseconds()
            if (expiry <= currentTime) {
                return false // Expired
            }
        }

        // Active or cancelled (but still valid until expiry) subscriptions are considered active
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.CANCELLED
    }
}
