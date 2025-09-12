package com.yral.shared.features.wallet.domain.models

data class BtcInInr(
    val priceInInr: Double,
)

data class UserBtcBalance(
    val balanceInSats: Long,
)
