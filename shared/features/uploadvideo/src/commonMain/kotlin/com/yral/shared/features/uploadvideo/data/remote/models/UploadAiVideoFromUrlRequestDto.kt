package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UploadAiVideoFromUrlRequestDto(
    @SerialName("video_url") val videoUrl: String,
    @SerialName("hashtags") val hashtags: List<String>,
    @SerialName("description") val description: String,
    @SerialName("delegated_identity_wire") val delegatedIdentityWire: KotlinDelegatedIdentityWire,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("enable_hot_or_not") val enableHotOrNot: Boolean,
)

@Suppress("MaxLineLength")
internal fun UploadAiVideoFromUrlRequest.toDto(delegatedIdentityWire: KotlinDelegatedIdentityWire): UploadAiVideoFromUrlRequestDto =
    UploadAiVideoFromUrlRequestDto(
        videoUrl = videoUrl,
        hashtags = hashtags,
        description = description,
        delegatedIdentityWire = delegatedIdentityWire,
        isNsfw = isNsfw,
        enableHotOrNot = enableHotOrNot,
    )
