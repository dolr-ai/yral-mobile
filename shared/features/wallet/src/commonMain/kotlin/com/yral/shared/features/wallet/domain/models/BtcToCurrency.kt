package com.yral.shared.features.wallet.domain.models

data class BtcToCurrency(
    val conversionRate: Double,
    val currencyCode: String,
)
