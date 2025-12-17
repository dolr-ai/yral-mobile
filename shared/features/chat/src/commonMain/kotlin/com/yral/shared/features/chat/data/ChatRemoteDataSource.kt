package com.yral.shared.features.chat.data

import com.yral.shared.core.AppConfigurations.CHAT_BASE_URL
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.http.httpGet
import io.ktor.client.HttpClient
import io.ktor.http.path
import kotlinx.serialization.json.Json

class ChatRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : ChatDataSource {
    override suspend fun listInfluencers(
        limit: Int,
        offset: Int,
    ): InfluencersResponseDto =
        httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(INFLUENCERS_PATH)
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
            }
        }

    override suspend fun getInfluencer(id: String): InfluencerDto =
        httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(INFLUENCERS_PATH, id)
            }
        }

    private companion object {
        private const val INFLUENCERS_PATH = "api/v1/influencers"
    }
}
