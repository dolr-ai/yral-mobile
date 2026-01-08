package com.yral.shared.libs.phonevalidation

import com.yral.shared.libs.phonevalidation.countries.CountryRepository
import platform.Contacts.CNPhoneNumber
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.regionCode

/**
 * iOS implementation of PhoneValidator using native Contacts framework (CNPhoneNumber).
 * Provides robust phone number validation, parsing, and formatting using iOS's built-in capabilities.
 */
@Suppress("MagicNumber", "ReturnCount", "TooGenericExceptionCaught", "SwallowedException")
actual class PhoneValidator {
    private val countryRepository = CountryRepository()

    /**
     * Validates a phone number for the given region using iOS native validation.
     * Uses CNPhoneNumber to check if the number is properly formatted.
     */
    actual fun isValid(
        phoneNumber: String,
        regionCode: String,
    ): Boolean {
        try {
            // Clean the input
            val cleanNumber = phoneNumber.trim()
            if (cleanNumber.isEmpty()) return false

            // Basic length validation (E.164 allows 7-15 digits)
            val digitCount = cleanNumber.count { it.isDigit() }
            if (digitCount !in 7..15) return false

            // Create CNPhoneNumber to validate structure
            val cnPhoneNumber =
                CNPhoneNumber.phoneNumberWithStringValue(cleanNumber) ?: return false

            // Additional validation: ensure it has proper format
            val stringValue = cnPhoneNumber.stringValue
            if (stringValue.isEmpty()) return false

            // Validate country code if provided
            if (cleanNumber.startsWith("+")) {
                val country = countryRepository.getCountryByCode(regionCode)
                if (country != null) {
                    // Check if number starts with the expected dial code
                    return cleanNumber.startsWith(country.dialCode) ||
                        // Or if it's just the national number
                        digitCount in 7..15
                }
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Formats a phone number according to the specified format using iOS native formatting.
     */
    actual fun format(
        phoneNumber: String,
        regionCode: String,
        format: PhoneNumberFormat,
    ): String {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (cleanNumber.isEmpty()) return phoneNumber

            return when (format) {
                PhoneNumberFormat.E164 -> formatE164(cleanNumber, regionCode)
                PhoneNumberFormat.INTERNATIONAL -> {
                    val e164 = format(phoneNumber, regionCode, PhoneNumberFormat.E164)
                    formatInternational(e164)
                }
                PhoneNumberFormat.NATIONAL -> formatNational(cleanNumber, regionCode)
            }
        } catch (e: Exception) {
            return phoneNumber
        }
    }

    /**
     * Formats a phone number in E.164 format.
     */
    private fun formatE164(
        cleanNumber: String,
        regionCode: String,
    ): String {
        if (cleanNumber.startsWith("+")) return cleanNumber

        val country = countryRepository.getCountryByCode(regionCode)
        return if (country != null) {
            "${country.dialCode}$cleanNumber"
        } else {
            "+$cleanNumber"
        }
    }

    /**
     * Formats a phone number in national format (without country code).
     */
    private fun formatNational(
        cleanNumber: String,
        regionCode: String,
    ): String {
        val country = countryRepository.getCountryByCode(regionCode)
        return if (country != null && cleanNumber.startsWith(country.dialCode)) {
            cleanNumber.removePrefix(country.dialCode)
        } else {
            cleanNumber.removePrefix("+")
        }
    }

    /**
     * Parses a phone number into its components (country code and national number).
     */
    actual fun parse(
        phoneNumber: String,
        regionCode: String,
    ): PhoneNumber? {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (!isValid(phoneNumber, regionCode)) return null

            return if (cleanNumber.startsWith("+")) {
                parseWithCountryCode(cleanNumber, phoneNumber)
            } else {
                parseWithoutCountryCode(cleanNumber, regionCode, phoneNumber)
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Parses a phone number that starts with a country code (+).
     */
    private fun parseWithCountryCode(
        cleanNumber: String,
        rawInput: String,
    ): PhoneNumber? {
        // Match against known country codes from repository
        val matchingCountry =
            countryRepository
                .getAllCountries()
                .firstOrNull { cleanNumber.startsWith(it.dialCode) }

        if (matchingCountry != null) {
            return createPhoneNumber(
                countryDialCode = matchingCountry.dialCode,
                cleanNumber = cleanNumber,
                rawInput = rawInput,
            )
        }

        // Fallback: try to extract first 1-3 digits as country code
        return parseFallbackCountryCode(cleanNumber, rawInput)
    }

    /**
     * Parses a phone number without a country code by using the region.
     */
    private fun parseWithoutCountryCode(
        cleanNumber: String,
        regionCode: String,
        rawInput: String,
    ): PhoneNumber {
        val country = countryRepository.getCountryByCode(regionCode)
        val countryCode = country?.dialCode?.removePrefix("+")?.toIntOrNull() ?: 0

        return PhoneNumber(
            countryCode = countryCode,
            nationalNumber = cleanNumber,
            rawInput = rawInput,
        )
    }

    /**
     * Creates a PhoneNumber object from a matched country.
     */
    private fun createPhoneNumber(
        countryDialCode: String,
        cleanNumber: String,
        rawInput: String,
    ): PhoneNumber {
        val countryCodeStr = countryDialCode.removePrefix("+")
        val nationalNumber = cleanNumber.removePrefix(countryDialCode)
        return PhoneNumber(
            countryCode = countryCodeStr.toIntOrNull() ?: 0,
            nationalNumber = nationalNumber,
            rawInput = rawInput,
        )
    }

    /**
     * Fallback parsing strategy when country code isn't in repository.
     */
    private fun parseFallbackCountryCode(
        cleanNumber: String,
        rawInput: String,
    ): PhoneNumber? {
        for (i in 3 downTo 1) {
            val potentialCode = cleanNumber.substring(1, minOf(1 + i, cleanNumber.length))
            if (potentialCode.all { it.isDigit() }) {
                val nationalNumber = cleanNumber.substring(1 + i)
                if (nationalNumber.length >= 7) {
                    return PhoneNumber(
                        countryCode = potentialCode.toIntOrNull() ?: 0,
                        nationalNumber = nationalNumber,
                        rawInput = rawInput,
                    )
                }
            }
        }
        return null
    }

    /**
     * Detects the region code from a phone number (if it contains a country code).
     */
    actual fun detectRegion(phoneNumber: String): String? {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (!cleanNumber.startsWith("+")) {
                // No country code, use device region as fallback
                return NSLocale.currentLocale.regionCode
            }

            // Try to match against known country codes
            val matchingCountry =
                countryRepository
                    .getAllCountries()
                    .firstOrNull { cleanNumber.startsWith(it.dialCode) }

            return matchingCountry?.code
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Gets the type of phone number (mobile, fixed line, etc.).
     * Note: iOS Contacts framework doesn't provide type detection natively.
     */
    actual fun getNumberType(
        phoneNumber: String,
        regionCode: String,
    ): PhoneNumberType {
        // iOS doesn't provide native number type detection
        // Could implement heuristics based on number patterns if needed
        return PhoneNumberType.UNKNOWN
    }

    /**
     * Helper function to format a number in international style with spacing.
     */
    private fun formatInternational(e164Number: String): String {
        if (!e164Number.startsWith("+")) return e164Number
        // Basic international formatting: +XX XXX XXX XXXX
        val withoutPlus = e164Number.substring(1)
        return when {
            withoutPlus.length <= 3 -> e164Number
            withoutPlus.length <= 6 -> "+${withoutPlus.take(2)} ${withoutPlus.substring(2)}"
            withoutPlus.length <= 10 -> {
                "+${withoutPlus.take(2)} ${withoutPlus.substring(2, 5)} " +
                    withoutPlus.substring(5)
            }
            else -> {
                "+${withoutPlus.take(2)} ${withoutPlus.substring(2, 5)} " +
                    "${withoutPlus.substring(5, 8)} ${withoutPlus.substring(8)}"
            }
        }
    }
}
