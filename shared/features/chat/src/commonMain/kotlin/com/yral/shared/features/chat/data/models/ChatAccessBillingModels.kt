package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GrantChatAccessRequestDto(
    @SerialName("package_name") val packageName: String,
    @SerialName("product_id") val productId: String,
    @SerialName("purchase_token") val purchaseToken: String,
    @SerialName("bot_id") val botId: String,
)

@Serializable
data class ChatAccessApiResponse(
    val success: Boolean,
    val msg: String? = null,
    val error: String? = null,
    val data: ChatAccessDataDto? = null,
)

data class GrantResult(
    val httpStatus: Int,
    val apiResponse: ChatAccessApiResponse,
)

@Serializable
data class ChatAccessDataDto(
    @SerialName("has_access") val hasAccess: Boolean,
    @SerialName("expires_at") val expiresAt: String? = null,
)
