package com.yral.shared.features.wallet.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillingTransactionsResponseDto(
    val success: Boolean,
    val data: List<TransactionResponseDto>? = null,
    val msg: String? = null,
    val error: String? = null,
)

@Serializable
data class TransactionResponseDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("transaction_type") val transactionType: String,
    @SerialName("amount_paise") val amountPaise: Long,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("purchase_token") val purchaseToken: String,
    @SerialName("created_at") val createdAt: String,
)
