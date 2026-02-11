package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SubscriptionEntryPoint {
    @SerialName("home_feed")
    HOME_FEED,

    @SerialName("profile")
    PROFILE,

    @SerialName("hamburger")
    HAMBURGER,

    @SerialName("ai_video")
    AI_VIDEO,

    @SerialName("ai_chatbot")
    AI_CHATBOT,

    @SerialName("tournament")
    TOURNAMENT,
}

@Serializable
enum class PaymentProvider {
    @SerialName("apple")
    APPLE,

    @SerialName("google")
    GOOGLE,
}

@Serializable
enum class PaymentStatus {
    @SerialName("success")
    SUCCESS,

    @SerialName("failure")
    FAILURE,
}

@Serializable
enum class ProStatusSource {
    @SerialName("purchase")
    PURCHASE,

    @SerialName("renewal")
    RENEWAL,
}

@Serializable
enum class CreditFeature {
    @SerialName("ai_video")
    AI_VIDEO,

    @SerialName("tournament")
    TOURNAMENT,

    @SerialName("chatbot")
    CHATBOT,
}

// 1. Pro Nudge Impression
@Serializable
data class ProNudgeImpressionEventData(
    @SerialName("event") override val event: String = FeatureEvents.PRO_NUDGE_IMPRESSION.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("entry_point") val entryPoint: SubscriptionEntryPoint,
) : BaseEventData(),
    EventData

// 2. Pro Nudge Clicked
@Serializable
data class ProNudgeClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.PRO_NUDGE_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("entry_point") val entryPoint: SubscriptionEntryPoint,
) : BaseEventData(),
    EventData

// 3. Pro Plan Viewed
@Serializable
data class ProPlanViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.PRO_PLAN_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("entry_point") val entryPoint: SubscriptionEntryPoint,
    @SerialName("plan_price") val planPrice: Double,
    @SerialName("credits_offered") val creditsOffered: Int,
) : BaseEventData(),
    EventData

// 4. Pro Buy Clicked
@Serializable
data class ProBuyClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.PRO_BUY_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("plan_price") val planPrice: Double,
    @SerialName("payment_provider") val paymentProvider: PaymentProvider,
) : BaseEventData(),
    EventData

// 5. Pro Payment Result
@Serializable
data class ProPaymentResultEventData(
    @SerialName("event") override val event: String = FeatureEvents.PRO_PAYMENT_RESULT.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("payment_status") val paymentStatus: PaymentStatus,
    @SerialName("amount_paid") val amountPaid: Double? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("transaction_id") val transactionId: String? = null,
) : BaseEventData(),
    EventData

// 6. Pro Status Updated
@Serializable
data class ProStatusUpdatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.PRO_STATUS_UPDATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("pro_status") val proStatus: Boolean,
    @SerialName("credits_granted") val creditsGranted: Int,
    @SerialName("source") val source: ProStatusSource,
) : BaseEventData(),
    EventData

// 7. Credits Consumed
@Serializable
data class CreditsConsumedEventData(
    @SerialName("event") override val event: String = FeatureEvents.CREDITS_CONSUMED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("feature") val feature: CreditFeature,
    @SerialName("credits_used") val creditsUsed: Int,
    @SerialName("credits_remaining") val creditsRemaining: Int,
) : BaseEventData(),
    EventData

// 8. Credits Exhausted
@Serializable
data class CreditsExhaustedEventData(
    @SerialName("event") override val event: String = FeatureEvents.CREDITS_EXHAUSTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.SUBSCRIPTION.getFeatureName(),
    @SerialName("last_used_feature") val lastUsedFeature: CreditFeature,
) : BaseEventData(),
    EventData
