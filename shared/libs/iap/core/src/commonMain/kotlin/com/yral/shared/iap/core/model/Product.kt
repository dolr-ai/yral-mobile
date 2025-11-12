package com.yral.shared.iap.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val price: String,
    val priceAmountMicros: Long,
    val currencyCode: String,
    val title: String,
    val description: String,
    val type: ProductType,
)
