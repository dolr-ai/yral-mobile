package com.yral.shared.features.wallet.data.models

import com.yral.shared.features.wallet.domain.models.BtcInInr
import com.yral.shared.features.wallet.domain.models.UserBtcBalance
import kotlinx.serialization.Serializable

@Serializable
data class GetBalanceResponseDto(
    val balance: Long,
)

@Serializable
data class BtcPriceResponseDto(
    val inr: Double,
)

fun BtcPriceResponseDto.toBtcInInr(): BtcInInr = BtcInInr(priceInInr = this.inr)

fun GetBalanceResponseDto.toUserBtcBalance(): UserBtcBalance = UserBtcBalance(balanceInSats = this.balance)
