package com.yral.shared.iap.core.providers

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

    val multiplier =
        when (unit) {
            "D" -> MILLIS_PER_DAY
            "W" -> MILLIS_PER_DAY * 7L
            "M" -> MILLIS_PER_DAY * 30L
            "Y" -> MILLIS_PER_DAY * 365L
            else -> return null
        }

    return amount.toLong() * multiplier
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
