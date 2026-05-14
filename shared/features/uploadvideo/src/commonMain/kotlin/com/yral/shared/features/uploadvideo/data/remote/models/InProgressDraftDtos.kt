package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.uploadvideo.domain.models.InProgressDraft
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class InProgressDraftsRequestDto(
    @SerialName("delegated_identity") val delegatedIdentity: KotlinDelegatedIdentityWire,
    @SerialName("user_id") val userId: String,
)

@Serializable
internal data class InProgressDraftsResponseDto(
    @SerialName("items") val items: List<InProgressDraftItemDto>,
)

@Serializable
internal data class InProgressDraftItemDto(
    @SerialName("created_at") val createdAt: String,
    @SerialName("model_id") val modelId: String,
    @SerialName("operation_id") val operationId: String,
    @SerialName("prompt") val prompt: String,
    @SerialName("provider") val provider: String? = null,
    @SerialName("status") val status: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
)

internal fun InProgressDraftsResponseDto.toDomain(): List<InProgressDraft> =
    items.map {
        InProgressDraft(
            createdAt = it.createdAt,
            modelId = it.modelId,
            operationId = it.operationId,
            prompt = it.prompt,
            provider = it.provider,
            status = it.status,
            thumbnailUrl = it.thumbnailUrl,
        )
    }
