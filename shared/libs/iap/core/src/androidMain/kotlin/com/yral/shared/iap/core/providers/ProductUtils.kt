package com.yral.shared.iap.core.providers

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal fun <T> resolveBaseAndPromo(
    offers: List<T>?,
    offerIdSelector: (T) -> String?,
): Pair<T?, T?> {
    if (offers == null) return null to null
    val baseOffer = offers.firstOrNull { offerIdSelector(it).isNullOrEmpty() }
    val promoOffer = offers.firstOrNull { !offerIdSelector(it).isNullOrEmpty() }
    return baseOffer to promoOffer
}

internal data class ResolvedPrices<R>(
    val basePrice: R,
    val offerPrice: R,
)

internal fun <T, R> resolveBaseAndOfferPrices(
    baseOffer: T?,
    promoOffer: T?,
    priceSelector: (T) -> R,
    default: R,
): ResolvedPrices<R> {
    val basePrice = (baseOffer ?: promoOffer)?.let(priceSelector) ?: default
    val offerPrice = promoOffer?.let(priceSelector) ?: basePrice
    return ResolvedPrices(
        basePrice = basePrice,
        offerPrice = offerPrice,
    )
}

// Google Play subscription billing period is an ISO-8601 duration like P1W, P1M, P3M, P1Y.
// This helper converts it to an approximate duration in milliseconds for UI purposes.
@Suppress("ReturnCount", "MagicNumber")
internal fun billingPeriodToDurationMillis(billingPeriod: String?): Long? {
    val value = billingPeriod ?: return null
    val regex = Regex("""^P(\d+)([DWMY])$""")
    val match = regex.matchEntire(value) ?: return null

    val amount = match.groupValues[1].toIntOrNull() ?: return null
    val unit = match.groupValues[2]

    val duration: Duration =
        when (unit) {
            "D" -> amount.days
            "W" -> (amount * 7).days
            "M" -> (amount * 30).days // approximate month
            "Y" -> (amount * 365).days // approximate year
            else -> Duration.ZERO
        }

    if (duration == Duration.ZERO) return null
    return duration.inWholeMilliseconds
}
