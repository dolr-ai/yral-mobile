package com.yral.shared.features.uploadvideo.domain.models

import kotlinx.serialization.json.JsonObject

data class Provider(
    val id: String,
    val name: String,
    val description: String?,
    val cost: ProviderCost?,
    val supportsImage: Boolean?,
    val supportsNegativePrompt: Boolean?,
    val supportsAudio: Boolean?,
    val supportsSeed: Boolean?,
    val allowedAspectRatios: List<String>,
    val allowedResolutions: List<String>,
    val allowedDurations: List<Int>,
    val defaultAspectRatio: String?,
    val defaultResolution: String?,
    val defaultDuration: Int?,
    val isAvailable: Boolean?,
    val isInternal: Boolean?,
    val modelIcon: String?,
    val extraInfo: JsonObject?,
) {
    fun toDefaultAspectRatio(): Float {
        var ratio = defaultAspectRatio
        if (ratio == null && allowedAspectRatios.isNotEmpty()) {
            ratio = allowedAspectRatios[0]
        }
        return ratio
            ?.split(":")
            ?.let { parts -> parts[0].toFloat() / parts[1].toFloat() }
            ?: DEFAULT_ASPECT_RATIO_FLOAT
    }

    companion object {
        private const val DEFAULT_ASPECT_RATIO_FLOAT = 1.78f // 16:9
    }
}

data class ProviderCost(
    val usdCents: Int?,
    val dolr: Long?,
    val sats: Long?,
)
