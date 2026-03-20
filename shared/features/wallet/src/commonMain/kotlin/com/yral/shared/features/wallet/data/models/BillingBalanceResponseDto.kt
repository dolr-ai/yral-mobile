package com.yral.shared.features.wallet.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillingBalanceResponseDto(
    val success: Boolean,
    val data: BillingBalanceDataDto? = null,
    val msg: String? = null,
    val error: String? = null,
)

@Serializable
data class BillingBalanceDataDto(
    @SerialName("balance_paise") val balancePaise: Long,
    @SerialName("balance_rupees") val balanceRupees: Double,
)
