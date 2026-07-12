package com.yral.shared.features.chat.data.models

import com.yral.shared.features.chat.domain.models.AssistantError
import com.yral.shared.features.chat.domain.models.AssistantErrorCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape for the SSE `error` event payload (and, when the backend wires
 * it, the non-streaming response-body error). Decoded by
 * [com.yral.shared.features.chat.data.ChatStreamingDataSource] and mapped
 * to the domain [AssistantError] via [toDomain].
 */
@Serializable
data class AssistantErrorDto(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String,
    @SerialName("retryable") val retryable: Boolean,
)

fun AssistantErrorDto.toDomain(): AssistantError =
    AssistantError(
        code = AssistantErrorCode.fromRaw(code),
        rawCode = code,
        message = message,
        retryable = retryable,
    )
