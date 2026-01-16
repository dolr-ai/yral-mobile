package com.yral.shared.libs.phonevalidation.countries

/**
 * Represents a country with its code, name, dial code, flag information, and phone number length constraints.
 * @param code ISO 3166-1 alpha-2 (e.g., "IN", "US")
 * @param name "India", "United States"
 * @param dialCode "+91", "+1"
 * @param flagUrl flagcdn.com URL for the country flag
 * @param minLength Minimum length of phone number (excluding country code)
 * @param maxLength Maximum length of phone number (excluding country code)
 */
data class Country(
    val code: String,
    val name: String,
    val dialCode: String,
    val flagUrl: String,
    val minLength: Int = 7,
    val maxLength: Int = 15,
) {
    companion object {
        // Helper to generate flag CDN URL
        fun getFlagUrl(countryCode: String): String = "https://flagcdn.com/w80/${countryCode.lowercase()}.png"
    }
}
