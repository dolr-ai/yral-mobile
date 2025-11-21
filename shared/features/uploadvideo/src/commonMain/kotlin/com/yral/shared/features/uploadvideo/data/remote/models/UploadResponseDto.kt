package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import kotlinx.serialization.Serializable

@Serializable
internal data class GetUploadUrlResponseDTO(
    val message: String?,
    val success: Boolean,
    val data: UploadDataDto,
)

@Serializable
internal data class UploadDataDto(
    val scheduledDeletion: String?,
    val uid: String,
    val uploadURL: String,
    val watermark: WatermarkDto?,
)

@Serializable
internal data class WatermarkDto(
    val created: String,
    val downloadedFrom: String?,
    val height: Double,
    val name: String,
    val opacity: Double,
    val padding: Double,
    val position: String,
    val scale: Double,
    val size: Double,
    val uid: String,
    val width: Double,
)

internal fun GetUploadUrlResponseDTO.toUploadEndpoint() =
    UploadEndpoint(
        data.uploadURL,
        data.uid,
    )
