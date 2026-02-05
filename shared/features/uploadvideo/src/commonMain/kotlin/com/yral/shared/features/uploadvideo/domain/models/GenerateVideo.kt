package com.yral.shared.features.uploadvideo.domain.models

import com.yral.shared.features.uploadvideo.data.remote.models.TokenType
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey

data class GenerateVideoParams(
    val providerId: String,
    val prompt: String,
    val negativePrompt: String? = null,
    val aspectRatio: String? = null,
    val resolution: String? = null,
    val seed: Long? = null,
    val audioUrl: String? = null,
    val durationSeconds: Int? = null,
    val generateAudio: Boolean? = null,
    val image: ImageData? = null,
    val tokenType: TokenType? = null,
    val userId: String? = null,
    val extraParams: Map<String, String>? = null,
)

sealed interface ImageData {
    data class Base64(
        val image: ImageInput,
    ) : ImageData
}

data class ImageInput(
    val data: String,
    val mimeType: String,
)

data class GenerateVideoResult(
    val operationId: String?,
    val provider: String?,
    val requestKey: VideoGenRequestKey?,
    val providerError: String?,
)
