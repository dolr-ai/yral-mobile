package com.yral.shared.libs.phonevalidation

data class PhoneNumber(
    val countryCode: Int,
    val nationalNumber: String,
    val rawInput: String,
)
