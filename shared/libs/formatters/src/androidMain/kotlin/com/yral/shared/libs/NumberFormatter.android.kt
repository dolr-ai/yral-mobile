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
            localeCode?.let {
                NumberFormat.getNumberInstance(Locale.forLanguageTag(it))
            } ?: NumberFormat.getNumberInstance()
        minimumFractionDigits?.let { numberFormat.minimumFractionDigits = it }
        maximumFractionDigits?.let { numberFormat.maximumFractionDigits = it }

        return numberFormat.format(value)
    }

    override fun format(
        value: Int,
        localeCode: String?,
    ): String {
        val numberFormat =
            localeCode?.let {
                NumberFormat.getNumberInstance(Locale.forLanguageTag(it))
            } ?: NumberFormat.getNumberInstance()

        return numberFormat.format(value)
    }

    override fun format(
        value: Long,
        localeCode: String?,
    ): String {
        val numberFormat =
            localeCode?.let {
                NumberFormat.getNumberInstance(Locale.forLanguageTag(it))
            } ?: NumberFormat.getNumberInstance()

        return numberFormat.format(value)
    }

    override fun parseAsDouble(
        text: String,
        localeCode: String?,
    ): Double? {
        val numberFormat =
            localeCode?.let {
                NumberFormat.getNumberInstance(Locale.forLanguageTag(it))
            } ?: NumberFormat.getNumberInstance()

        return try {
            numberFormat.parse(text)?.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    override fun parseAsInt(
        text: String,
        localeCode: String?,
    ): Int? {
        val numberFormat =
            localeCode?.let {
                NumberFormat.getNumberInstance(Locale.forLanguageTag(it))
            } ?: NumberFormat.getNumberInstance()

        return try {
            numberFormat.parse(text)?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    override fun parseAsLong(
        text: String,
        localeCode: String?,
    ): Long? {
        val numberFormat =
            localeCode?.let {
                NumberFormat.getNumberInstance(Locale.forLanguageTag(it))
            } ?: NumberFormat.getNumberInstance()

        return try {
            numberFormat.parse(text)?.toLong()
        } catch (e: Exception) {
            null
        }
    }
}
