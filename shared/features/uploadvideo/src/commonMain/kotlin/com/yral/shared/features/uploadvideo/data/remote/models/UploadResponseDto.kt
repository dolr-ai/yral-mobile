package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetUploadUrlResponseDTO(
    @SerialName("error_message")
    val errorMessage: String?,
    val success: Boolean,
    val data: UploadDataDto,
)

@Serializable
internal data class UploadDataDto(
    @SerialName("upload_url")
    val uploadUrl: String,
    @SerialName("video_id")
    val videoId: String,
)

internal fun GetUploadUrlResponseDTO.toUploadEndpoint() =
    UploadEndpoint(
        data.uploadUrl,
        data.videoId,
    )
