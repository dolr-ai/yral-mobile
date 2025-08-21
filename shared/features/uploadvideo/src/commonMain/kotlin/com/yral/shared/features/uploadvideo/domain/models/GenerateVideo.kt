package com.yral.shared.features.uploadvideo.domain.models

import com.yral.shared.uniffi.generated.VideoGenRequestKey

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
    val image: String? = null,
    val tokenType: String? = null,
    val userId: String? = null,
    val extraParams: Map<String, String>? = null,
)

data class GenerateVideoResult(
    val operationId: String?,
    val provider: String?,
    val requestKey: VideoGenRequestKey?,
    val providerError: String?,
)
