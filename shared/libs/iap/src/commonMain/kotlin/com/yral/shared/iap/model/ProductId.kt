package com.yral.shared.iap.model

import kotlinx.serialization.Serializable

/**
 * Enum representing all available in-app purchase product IDs.
 * This provides type safety and maintainability over raw strings.
 *
 * To add a new product:
 * 1. Add a new enum entry with the product ID string
 * 2. The productId property will automatically use the enum name as the ID
 *    or you can override it with a custom string
 */
@Serializable
enum class ProductId(
    val productId: String,
) {
    // Example product IDs - replace with your actual product IDs
    // For Android: these should match your product IDs in Google Play Console
    // For iOS: these should match your product IDs in App Store Connect
    // Subscription products
    PREMIUM_MONTHLY("premium_monthly"),
    PREMIUM_YEARLY("premium_yearly"),

    // One-time purchase products
    REMOVE_ADS("remove_ads"),

    // Add more products as needed
    ;

    companion object {
        /**
         * Finds a ProductId by its string product ID
         */
        fun fromString(productId: String): ProductId? = entries.find { it.productId == productId }
    }
}
