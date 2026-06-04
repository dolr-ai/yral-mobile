package com.yral.shared.features.profile.videoideas.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.profile.videoideas.data.models.ListVideoIdeasResponseDto
import com.yral.shared.features.profile.videoideas.data.models.MarkVideoIdeaUsedResponseDto
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import kotlinx.serialization.json.Json

class VideoIdeasRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val chatBaseUrl: String,
) : VideoIdeasDataSource {
    override suspend fun listIdeas(influencerId: String): ListVideoIdeasResponseDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(INFLUENCERS_PATH, influencerId, VIDEO_IDEAS_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun markIdeaUsed(
        influencerId: String,
        ideaId: String,
    ): MarkVideoIdeaUsedResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(INFLUENCERS_PATH, influencerId, VIDEO_IDEAS_PATH, ideaId, USED_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    private suspend fun getIdToken() =
        preferences.getString(PrefKeys.ID_TOKEN.name)
            ?: throw YralException("Authorisation not found")

    private companion object {
        const val INFLUENCERS_PATH = "api/v1/influencers"
        const val VIDEO_IDEAS_PATH = "video-ideas"
        const val USED_PATH = "used"
    }
}
