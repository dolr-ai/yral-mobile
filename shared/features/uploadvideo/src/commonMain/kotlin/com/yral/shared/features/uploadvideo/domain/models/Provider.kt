package com.yral.shared.features.uploadvideo.domain.models

import kotlinx.serialization.json.JsonObject

data class Provider(
    val id: String,
    val name: String,
    val description: String?,
    val cost: ProviderCost?,
    val supportsImage: Boolean,
    val supportsNegativePrompt: Boolean,
    val supportsAudio: Boolean,
    val supportsSeed: Boolean,
    val allowedAspectRatios: List<String>,
    val allowedResolutions: List<String>,
    val allowedDurations: List<Int>,
    val defaultAspectRatio: String?,
    val defaultResolution: String?,
    val defaultDuration: Int?,
    val isAvailable: Boolean,
    val isInternal: Boolean,
    val modelIcon: String?,
    val extraInfo: JsonObject?,
)

data class ProviderCost(
    val usdCents: Int?,
    val dolr: Long?,
    val sats: Long?,
)
