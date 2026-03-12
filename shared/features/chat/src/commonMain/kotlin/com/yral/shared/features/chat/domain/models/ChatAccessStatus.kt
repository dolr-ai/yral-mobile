package com.yral.shared.features.chat.domain.models

data class ChatAccessStatus(
    val hasAccess: Boolean,
    val expiresAtMs: Long? = null,
)

sealed class GrantError(
    message: String,
) : Exception(message) {
    class ClientError(
        val errorMsg: String,
    ) : GrantError("Grant rejected: $errorMsg")

    class ServerError(
        val httpStatus: Int,
    ) : GrantError("Server error: $httpStatus")
}
