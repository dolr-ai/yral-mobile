package com.yral.shared.libs.phonevalidation

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

actual class PhoneValidator {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    actual fun isValid(
        phoneNumber: String,
        regionCode: String,
    ): Boolean =
        try {
            val number = phoneUtil.parse(phoneNumber, regionCode)
            phoneUtil.isValidNumber(number)
        } catch (e: NumberParseException) {
            false
        }

    actual fun format(
        phoneNumber: String,
        regionCode: String,
        format: PhoneNumberFormat,
    ): String =
        try {
            val number = phoneUtil.parse(phoneNumber, regionCode)
            when (format) {
                PhoneNumberFormat.E164 ->
                    phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
                PhoneNumberFormat.INTERNATIONAL ->
                    phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                PhoneNumberFormat.NATIONAL ->
                    phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            }
        } catch (e: NumberParseException) {
            phoneNumber
        }

    actual fun parse(
        phoneNumber: String,
        regionCode: String,
    ): PhoneNumber? =
        try {
            val number = phoneUtil.parse(phoneNumber, regionCode)
            PhoneNumber(
                countryCode = number.countryCode,
                nationalNumber = number.nationalNumber.toString(),
                rawInput = phoneNumber,
            )
        } catch (e: NumberParseException) {
            null
        }

    actual fun detectRegion(phoneNumber: String): String? =
        try {
            val number = phoneUtil.parse(phoneNumber, null)
            phoneUtil.getRegionCodeForNumber(number)
        } catch (e: NumberParseException) {
            null
        }

    actual fun getNumberType(
        phoneNumber: String,
        regionCode: String,
    ): PhoneNumberType =
        try {
            val number = phoneUtil.parse(phoneNumber, regionCode)
            when (phoneUtil.getNumberType(number)) {
                PhoneNumberUtil.PhoneNumberType.MOBILE -> PhoneNumberType.MOBILE
                PhoneNumberUtil.PhoneNumberType.FIXED_LINE -> PhoneNumberType.FIXED_LINE
                PhoneNumberUtil.PhoneNumberType.TOLL_FREE -> PhoneNumberType.TOLL_FREE
                PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE -> PhoneNumberType.PREMIUM_RATE
                PhoneNumberUtil.PhoneNumberType.VOIP -> PhoneNumberType.VOIP
                else -> PhoneNumberType.UNKNOWN
            }
        } catch (e: NumberParseException) {
            PhoneNumberType.UNKNOWN
        }
}
