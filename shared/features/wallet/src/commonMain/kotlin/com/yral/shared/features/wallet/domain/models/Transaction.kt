package com.yral.shared.features.wallet.domain.models

data class Transaction(
    val id: String,
    val userId: String,
    val recipientId: String,
    val transactionType: String,
    val amountPaise: Long,
    val createdAt: String,
    val username: String? = null,
)
