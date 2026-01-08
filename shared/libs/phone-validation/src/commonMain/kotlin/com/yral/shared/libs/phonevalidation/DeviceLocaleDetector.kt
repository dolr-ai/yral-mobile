package com.yral.shared.libs.phonevalidation

expect class DeviceLocaleDetector {
    // Get device's current region/country code (ISO 3166-1 alpha-2)
    // Returns "IN", "US", "GB", etc.
    // No permissions required - uses system locale
    fun getDeviceRegionCode(): String?

    // Get device's language code (optional, for localization)
    fun getDeviceLanguageCode(): String?
}
