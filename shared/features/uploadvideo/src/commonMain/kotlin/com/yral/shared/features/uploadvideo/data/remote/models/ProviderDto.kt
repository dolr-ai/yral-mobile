package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.domain.models.ProviderCost
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ProvidersResponseDto(
    val providers: List<ProviderDto> = emptyList(),
)

@Serializable
internal data class ProviderDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val cost: ProviderCostDto? = null,
    @SerialName("supports_image") val supportsImage: Boolean? = null,
    @SerialName("supports_negative_prompt") val supportsNegativePrompt: Boolean? = null,
    @SerialName("supports_audio") val supportsAudio: Boolean? = null,
    @SerialName("supports_seed") val supportsSeed: Boolean? = null,
    @SerialName("allowed_aspect_ratios") val allowedAspectRatios: List<String> = emptyList(),
    @SerialName("allowed_resolutions") val allowedResolutions: List<String> = emptyList(),
    @SerialName("allowed_durations") val allowedDurations: List<Int> = emptyList(),
    @SerialName("default_aspect_ratio") val defaultAspectRatio: String? = null,
    @SerialName("default_resolution") val defaultResolution: String? = null,
    @SerialName("default_duration") val defaultDuration: Int? = null,
    @SerialName("is_available") val isAvailable: Boolean? = null,
    @SerialName("is_internal") val isInternal: Boolean? = null,
    @SerialName("model_icon") val modelIcon: String? = null,
    @SerialName("extra_info") val extraInfo: JsonObject? = null,
)

@Serializable
internal data class ProviderCostDto(
    @SerialName("usd_cents") val usdCents: Int? = null,
    val dolr: Long? = null,
    val sats: Long? = null,
)

internal fun ProvidersResponseDto.toDomain(): List<Provider> = providers.map { it.toDomain() }

internal fun ProviderDto.toDomain(): Provider =
    Provider(
        id = id,
        name = name,
        description = description,
        cost = cost?.toDomain(),
        supportsImage = supportsImage ?: false,
        supportsNegativePrompt = supportsNegativePrompt ?: false,
        supportsAudio = supportsAudio ?: false,
        supportsSeed = supportsSeed ?: false,
        allowedAspectRatios = allowedAspectRatios,
        allowedResolutions = allowedResolutions,
        allowedDurations = allowedDurations,
        defaultAspectRatio = defaultAspectRatio,
        defaultResolution = defaultResolution,
        defaultDuration = defaultDuration,
        isAvailable = isAvailable ?: true,
        isInternal = isInternal ?: false,
        modelIcon = modelIcon,
        extraInfo = extraInfo,
    )

private fun ProviderCostDto.toDomain(): ProviderCost =
    ProviderCost(
        usdCents = usdCents,
        dolr = dolr,
        sats = sats,
    )
