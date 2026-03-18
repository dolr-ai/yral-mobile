package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.features.uploadvideo.domain.models.ImageData
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

@Serializable
internal data class GenerateVideoRequestDto(
    @SerialName("delegated_identity") val delegatedIdentity: KotlinDelegatedIdentityWire,
    @SerialName("request") val request: RequestBodyDto,
    @SerialName("upload_handling") val uploadHandling: String? = null,
)

@Serializable
internal data class RequestBodyDto(
    @SerialName("aspect_ratio") val aspectRatio: String?,
    @SerialName("duration_seconds") val durationSeconds: Int?,
    // @SerialName("extra_params") val extraParams: Map<String, String>?,
    @SerialName("generate_audio") val generateAudio: Boolean?,
    @SerialName("model_id") val modelId: String,
    @SerialName("negative_prompt") val negativePrompt: String?,
    @SerialName("prompt") val prompt: String,
    @SerialName("image") val image: JsonElement?,
    @SerialName("resolution") val resolution: String?,
    @SerialName("seed") val seed: Long? = null,
    @SerialName("token_type") val tokenType: TokenType?,
    @SerialName("user_id") val userId: String,
)

@Serializable
enum class TokenType {
    @SerialName("Free")
    FREE,

    @SerialName("Sats")
    SATS,

    @SerialName("YralProSubscription")
    YRAL_PRO_SUBSCRIPTION,
}

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

@Serializable(with = VideoGenErrorDtoSerializer::class)
internal sealed class VideoGenErrorDto {
    data class ProviderError(
        val message: String,
    ) : VideoGenErrorDto()
    data class InvalidInput(
        val message: String,
    ) : VideoGenErrorDto()
    object AuthError : VideoGenErrorDto()
    data class NetworkError(
        val message: String,
    ) : VideoGenErrorDto()
    object InsufficientBalance : VideoGenErrorDto()
    object InvalidSignature : VideoGenErrorDto()
    data class UnsupportedModel(
        val model: String,
    ) : VideoGenErrorDto()

    val errorMessage: String
        get() =
            when (this) {
                is ProviderError -> message
                is InvalidInput -> message
                is AuthError -> "Authentication failed"
                is NetworkError -> message
                is InsufficientBalance -> "Insufficient balance"
                is InvalidSignature -> "Invalid signature"
                is UnsupportedModel -> "Unsupported model: $model"
            }
}

internal object VideoGenErrorDtoSerializer : KSerializer<VideoGenErrorDto> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("VideoGenErrorDto", PolymorphicKind.SEALED)

    @Suppress("CyclomaticComplexMethod")
    override fun deserialize(decoder: Decoder): VideoGenErrorDto {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("Only JSON decoding is supported")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive ->
                when (element.content) {
                    "AuthError" -> VideoGenErrorDto.AuthError
                    "InsufficientBalance" -> VideoGenErrorDto.InsufficientBalance
                    "InvalidSignature" -> VideoGenErrorDto.InvalidSignature
                    else -> throw SerializationException("Unknown unit variant: ${element.content}")
                }
            is JsonObject -> {
                val key =
                    element.keys.firstOrNull()
                        ?: throw SerializationException("Empty object for VideoGenError")
                val value =
                    (element[key] as? JsonPrimitive)?.content
                        ?: throw SerializationException("Expected string value for variant $key")
                when (key) {
                    "ProviderError" -> VideoGenErrorDto.ProviderError(value)
                    "InvalidInput" -> VideoGenErrorDto.InvalidInput(value)
                    "NetworkError" -> VideoGenErrorDto.NetworkError(value)
                    "UnsupportedModel" -> VideoGenErrorDto.UnsupportedModel(value)
                    else -> throw SerializationException("Unknown tuple variant: $key")
                }
            }
            else -> throw SerializationException("Unexpected JSON element type for VideoGenError")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: VideoGenErrorDto,
    ): Unit = throw UnsupportedOperationException("Serialization of VideoGenErrorDto is not supported")
}

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
                image = image?.toJsonElement(),
                modelId = providerId,
                negativePrompt = negativePrompt,
                prompt = prompt,
                resolution = resolution,
                seed = seed,
                tokenType = tokenType,
                userId = userId ?: "",
            ),
        uploadHandling = uploadHandling,
    )

private fun ImageData.toJsonElement(): JsonElement =
    when (this) {
        is ImageData.Base64 ->
            buildJsonObject {
                put("type", "Base64")
                put(
                    "value",
                    buildJsonObject {
                        put("data", image.data)
                        put("mime_type", image.mimeType)
                    },
                )
            }
    }

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
        val errorElement = runCatching { json.parseToJsonElement(text) }.getOrNull()
        val error =
            errorElement
                ?.let { runCatching { json.decodeFromJsonElement<VideoGenErrorDto>(it) }.getOrNull() }
        GenerateVideoResult(
            operationId = null,
            provider = null,
            requestKey = null,
            providerError = error?.errorMessage ?: text,
        )
    }
}
