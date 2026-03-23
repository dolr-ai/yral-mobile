package com.yral.shared.features.wallet.domain.models

data class Transaction(
    val id: String,
    val userId: String,
    val transactionType: String,
    val amountPaise: Long,
    val relatedBotId: String,
    val createdAt: String,
)
