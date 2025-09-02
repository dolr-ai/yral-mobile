package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
internal data class GenerateVideoRequestDto(
    @SerialName("delegated_identity") val delegatedIdentity: KotlinDelegatedIdentityWire,
    @SerialName("request") val request: RequestBodyDto,
)

@Serializable
internal data class RequestBodyDto(
    @SerialName("aspect_ratio") val aspectRatio: String?,
    @SerialName("duration_seconds") val durationSeconds: Int?,
    // @SerialName("extra_params") val extraParams: Map<String, String>?,
    @SerialName("generate_audio") val generateAudio: Boolean?,
    @SerialName("image") val image: String?,
    @SerialName("model_id") val modelId: String,
    @SerialName("negative_prompt") val negativePrompt: String?,
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String?,
    @SerialName("seed") val seed: Long? = null,
    @SerialName("token_type") val tokenType: String?,
    @SerialName("user_id") val userId: String,
)

@Serializable
internal data class GenerateVideoSuccessDto(
    @SerialName("operation_id") val operationId: String,
    @SerialName("provider") val provider: String,
    @SerialName("request_key") val requestKey: RequestKeyDto,
)

@Serializable
internal data class RequestKeyDto(
    @SerialName("counter") val counter: Long,
    @SerialName("principal") val principal: String,
)

@Serializable
internal data class GenerateVideoProviderErrorDto(
    @SerialName("ProviderError") val providerError: String,
)

@Suppress("MaxLineLength")
internal fun GenerateVideoParams.toRequestDto(delegatedIdentityWire: KotlinDelegatedIdentityWire): GenerateVideoRequestDto =
    GenerateVideoRequestDto(
        delegatedIdentity = delegatedIdentityWire,
        request =
            RequestBodyDto(
                aspectRatio = aspectRatio,
                durationSeconds = durationSeconds,
                // extraParams = extraParams,
                generateAudio = generateAudio,
                image = image,
                modelId = providerId,
                negativePrompt = negativePrompt,
                prompt = prompt,
                resolution = resolution,
                seed = seed,
                tokenType = tokenType,
                userId = userId ?: "",
            ),
    )

internal suspend fun HttpResponse.parseGenerateVideoResponse(json: Json): GenerateVideoResult {
    val text = bodyAsText()
    return if (status.isSuccess()) {
        val dto = json.decodeFromString(GenerateVideoSuccessDto.serializer(), text)
        GenerateVideoResult(
            operationId = dto.operationId,
            provider = dto.provider,
            requestKey =
                VideoGenRequestKey(
                    counter = dto.requestKey.counter.toULong(),
                    principal = dto.requestKey.principal,
                ),
            providerError = null,
        )
    } else {
        val errorObj = json.parseToJsonElement(text).jsonObject
        val error = json.decodeFromJsonElement<GenerateVideoProviderErrorDto>(errorObj)
        GenerateVideoResult(
            operationId = null,
            provider = null,
            requestKey = null,
            providerError = error.providerError,
        )
    }
}
