package com.yral.shared.features.aiinfluencer.data

import com.yral.shared.core.AppConfigurations.CHAT_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.aiinfluencer.data.models.GeneratePromptRequestDto
import com.yral.shared.features.aiinfluencer.data.models.GeneratePromptResponseDto
import com.yral.shared.features.aiinfluencer.data.models.ValidateAndGenerateMetadataRequestDto
import com.yral.shared.features.aiinfluencer.data.models.ValidateAndGenerateMetadataResponseDto
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import kotlinx.serialization.json.Json

class AiInfluencerRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val environmentPrefix: String,
) : AiInfluencerDataSource {
    override suspend fun generatePrompt(prompt: String): GeneratePromptResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(environmentPrefix, GENERATE_PROMPT_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            setBody(GeneratePromptRequestDto(prompt = prompt))
        }
    }

    @Suppress("MaxLineLength")
    override suspend fun validateAndGenerateMetadata(systemInstructions: String): ValidateAndGenerateMetadataResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(environmentPrefix, VALIDATE_AND_GENERATE_METADATA_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            setBody(
                ValidateAndGenerateMetadataRequestDto(
                    systemInstructions = systemInstructions,
                ),
            )
        }
    }

    private suspend fun getIdToken() =
        preferences.getString(PrefKeys.ID_TOKEN.name)
            ?: throw YralException("Authorisation not found")

    private companion object {
        private const val GENERATE_PROMPT_PATH = "api/v1/influencers/generate-prompt"
        private const val VALIDATE_AND_GENERATE_METADATA_PATH = "api/v1/influencers/validate-and-generate-metadata"
    }
}
