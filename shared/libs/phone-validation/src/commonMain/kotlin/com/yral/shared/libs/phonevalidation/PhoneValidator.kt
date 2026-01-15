package com.yral.shared.libs.phonevalidation

expect class PhoneValidator() {
    // Validate phone number format
    fun isValid(
        phoneNumber: String,
        regionCode: String,
    ): Boolean

    // Format numbers (add spaces, hyphens)
    fun format(
        phoneNumber: String,
        regionCode: String,
        format: PhoneNumberFormat,
    ): String

    // Parse and extract country code
    fun parse(
        phoneNumber: String,
        regionCode: String,
    ): PhoneNumber?

    // Auto-detect country from number
    fun detectRegion(phoneNumber: String): String?

    // Detect number type (mobile, landline, etc.)
    fun getNumberType(
        phoneNumber: String,
        regionCode: String,
    ): PhoneNumberType
}
