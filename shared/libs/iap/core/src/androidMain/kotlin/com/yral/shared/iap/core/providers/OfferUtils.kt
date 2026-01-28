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
