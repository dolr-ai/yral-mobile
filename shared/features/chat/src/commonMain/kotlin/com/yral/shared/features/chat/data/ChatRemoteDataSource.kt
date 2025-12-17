package com.yral.shared.features.chat.data

import com.yral.shared.core.AppConfigurations.CHAT_BASE_URL
import com.yral.shared.features.chat.data.models.ConversationDto
import com.yral.shared.features.chat.data.models.ConversationsResponseDto
import com.yral.shared.features.chat.data.models.CreateConversationRequestDto
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
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

    override suspend fun createConversation(influencerId: String): ConversationDto =
        httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(CONVERSATIONS_PATH)
            }
            setBody(
                CreateConversationRequestDto(
                    influencerId = influencerId,
                ),
            )
        }

    override suspend fun listConversations(
        limit: Int,
        offset: Int,
        influencerId: String?,
    ): ConversationsResponseDto =
        httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(CONVERSATIONS_PATH)
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
                if (!influencerId.isNullOrBlank()) {
                    parameters.append("influencer_id", influencerId)
                }
            }
        }

    private companion object {
        private const val INFLUENCERS_PATH = "api/v1/influencers"
        private const val CONVERSATIONS_PATH = "api/v1/chat/conversations"
    }
}
