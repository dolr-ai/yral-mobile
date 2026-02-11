package com.yral.shared.features.subscriptions.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.CreditFeature
import com.yral.shared.analytics.events.CreditsConsumedEventData
import com.yral.shared.analytics.events.CreditsExhaustedEventData
import com.yral.shared.analytics.events.PaymentProvider
import com.yral.shared.analytics.events.PaymentStatus
import com.yral.shared.analytics.events.ProBuyClickedEventData
import com.yral.shared.analytics.events.ProNudgeClickedEventData
import com.yral.shared.analytics.events.ProNudgeImpressionEventData
import com.yral.shared.analytics.events.ProPaymentResultEventData
import com.yral.shared.analytics.events.ProPlanViewedEventData
import com.yral.shared.analytics.events.ProStatusSource
import com.yral.shared.analytics.events.ProStatusUpdatedEventData
import com.yral.shared.analytics.events.SubscriptionEntryPoint

class SubscriptionTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onNudgeImpression(entryPoint: SubscriptionEntryPoint) {
        analyticsManager.trackEvent(ProNudgeImpressionEventData(entryPoint = entryPoint))
    }

    fun onNudgeClicked(entryPoint: SubscriptionEntryPoint) {
        analyticsManager.trackEvent(ProNudgeClickedEventData(entryPoint = entryPoint))
    }

    fun onPlanViewed(
        entryPoint: SubscriptionEntryPoint,
        planPrice: Double,
        creditsOffered: Int,
    ) {
        analyticsManager.trackEvent(
            ProPlanViewedEventData(
                entryPoint = entryPoint,
                planPrice = planPrice,
                creditsOffered = creditsOffered,
            ),
        )
    }

    fun onBuyClicked(
        planPrice: Double,
        paymentProvider: PaymentProvider,
    ) {
        analyticsManager.trackEvent(
            ProBuyClickedEventData(
                planPrice = planPrice,
                paymentProvider = paymentProvider,
            ),
        )
    }

    fun onPaymentResult(
        paymentStatus: PaymentStatus,
        amountPaid: Double? = null,
        currency: String? = null,
        transactionId: String? = null,
    ) {
        analyticsManager.trackEvent(
            ProPaymentResultEventData(
                paymentStatus = paymentStatus,
                amountPaid = amountPaid,
                currency = currency,
                transactionId = transactionId,
            ),
        )
    }

    fun onProStatusUpdated(
        proStatus: Boolean,
        creditsGranted: Int,
        source: ProStatusSource,
    ) {
        analyticsManager.trackEvent(
            ProStatusUpdatedEventData(
                proStatus = proStatus,
                creditsGranted = creditsGranted,
                source = source,
            ),
        )
    }

    fun onCreditsConsumed(
        feature: CreditFeature,
        creditsUsed: Int,
        creditsRemaining: Int,
    ) {
        analyticsManager.trackEvent(
            CreditsConsumedEventData(
                feature = feature,
                creditsUsed = creditsUsed,
                creditsRemaining = creditsRemaining,
            ),
        )
        // Fire credits_exhausted if remaining is 0
        if (creditsRemaining == 0) {
            analyticsManager.trackEvent(
                CreditsExhaustedEventData(lastUsedFeature = feature),
            )
        }
    }
}
