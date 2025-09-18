package com.yral.shared.libs

import java.text.NumberFormat
import java.util.Locale

actual fun NumberFormatter(): NumberFormatter = AndroidNumberFormatter

@Suppress("SwallowedException", "TooGenericExceptionCaught")
internal object AndroidNumberFormatter : NumberFormatter {
    override fun format(
        value: Double,
        localeCode: String?,
        minimumFractionDigits: Int?,
        maximumFractionDigits: Int?,
    ): String {
        val numberFormat =
            getNumberFormat(localeCode).apply {
                minimumFractionDigits?.let { this.minimumFractionDigits = it }
                maximumFractionDigits?.let { this.maximumFractionDigits = it }
            }
        return numberFormat.format(value)
    }

    override fun format(
        value: Int,
        localeCode: String?,
    ): String = getNumberFormat(localeCode).format(value)

    override fun format(
        value: Long,
        localeCode: String?,
    ): String = getNumberFormat(localeCode).format(value)

    override fun parseAsDouble(
        text: String,
        localeCode: String?,
    ): Double? =
        try {
            getNumberFormat(localeCode).parse(text)?.toDouble()
        } catch (e: Exception) {
            null
        }

    override fun parseAsInt(
        text: String,
        localeCode: String?,
    ): Int? =
        try {
            getNumberFormat(localeCode).parse(text)?.toInt()
        } catch (e: Exception) {
            null
        }

    override fun parseAsLong(
        text: String,
        localeCode: String?,
    ): Long? =
        try {
            getNumberFormat(localeCode).parse(text)?.toLong()
        } catch (e: Exception) {
            null
        }

    private fun getNumberFormat(localeCode: String?): NumberFormat =
        localeCode?.let {
            NumberFormat.getNumberInstance(Locale.forLanguageTag(it))
        } ?: NumberFormat.getNumberInstance()
}
