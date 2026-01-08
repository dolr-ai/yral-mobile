package com.yral.shared.libs.phonevalidation.countries

/**
 * Represents a country with its code, name, dial code, and flag information.
 * @param code ISO 3166-1 alpha-2 (e.g., "IN", "US")
 * @param name "India", "United States"
 * @param dialCode "+91", "+1"
 * @param flagUrl flagcdn.com URL for the country flag
 */
data class Country(
    val code: String,
    val name: String,
    val dialCode: String,
    val flagUrl: String,
) {
    companion object {
        // Helper to generate flag CDN URL
        fun getFlagUrl(countryCode: String): String = "https://flagcdn.com/w40/${countryCode.lowercase()}.png"
    }
}
