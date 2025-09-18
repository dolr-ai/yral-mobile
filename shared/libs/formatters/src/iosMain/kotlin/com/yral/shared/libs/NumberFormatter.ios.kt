package com.yral.shared.libs

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale

actual fun NumberFormatter(): NumberFormatter = IosNumberFormatter

internal object IosNumberFormatter : NumberFormatter {
    override fun format(
        value: Double,
        localeCode: String?,
        minimumFractionDigits: Int?,
        maximumFractionDigits: Int?,
    ): String {
        val numberFormat =
            getNumberFormat(localeCode).apply {
                minimumFractionDigits?.let { this.minimumFractionDigits = it.toULong() }
                maximumFractionDigits?.let { this.maximumFractionDigits = it.toULong() }
            }
        val decimalNumber = NSNumber(double = value)

        return numberFormat.stringFromNumber(decimalNumber) ?: value.toString()
    }

    override fun format(
        value: Int,
        localeCode: String?,
    ): String {
        val numberFormat = getNumberFormat(localeCode)
        val decimalNumber = NSNumber(int = value)
        return numberFormat.stringFromNumber(decimalNumber) ?: value.toString()
    }

    override fun format(
        value: Long,
        localeCode: String?,
    ): String {
        val numberFormat = getNumberFormat(localeCode)
        val decimalNumber = NSNumber(long = value)
        return numberFormat.stringFromNumber(decimalNumber) ?: value.toString()
    }

    override fun parseAsDouble(
        text: String,
        localeCode: String?,
    ): Double? = getNumberFormat(localeCode).numberFromString(text)?.doubleValue

    override fun parseAsInt(
        text: String,
        localeCode: String?,
    ): Int? = getNumberFormat(localeCode).numberFromString(text)?.intValue

    override fun parseAsLong(
        text: String,
        localeCode: String?,
    ): Long? = getNumberFormat(localeCode).numberFromString(text)?.longValue

    private fun getNumberFormat(localeCode: String?) =
        NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterDecimalStyle
            locale = localeCode?.let {
                NSLocale(localeIdentifier = it)
            } ?: NSLocale.currentLocale()
        }
}
