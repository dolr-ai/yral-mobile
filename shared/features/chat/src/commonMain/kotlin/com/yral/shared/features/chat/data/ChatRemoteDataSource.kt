package com.yral.shared.features.chat.data

import com.yral.shared.core.AppConfigurations.CHAT_BASE_URL
import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.data.models.ConversationDto
import com.yral.shared.features.chat.data.models.ConversationMessagesResponseDto
import com.yral.shared.features.chat.data.models.ConversationsResponseDto
import com.yral.shared.features.chat.data.models.CreateConversationRequestDto
import com.yral.shared.features.chat.data.models.DeleteConversationResponseDto
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageResponseDto
import com.yral.shared.features.chat.data.models.UploadResponseDto
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

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

    override suspend fun deleteConversation(conversationId: String): DeleteConversationResponseDto {
        val response =
            httpClient.delete {
                url {
                    host = CHAT_BASE_URL
                    path(CONVERSATIONS_PATH, conversationId)
                }
            }
        val deserializer = json.serializersModule.serializer<DeleteConversationResponseDto>()
        return json.decodeFromString(
            deserializer = deserializer,
            string = response.bodyAsText(),
        )
    }

    override suspend fun listConversationMessages(
        conversationId: String,
        limit: Int,
        offset: Int,
        order: String,
    ): ConversationMessagesResponseDto =
        httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(CONVERSATIONS_PATH, conversationId, MESSAGES_PATH)
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
                parameters.append("order", order)
            }
        }

    override suspend fun sendMessageJson(
        conversationId: String,
        request: SendMessageRequestDto,
    ): SendMessageResponseDto =
        httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = CHAT_BASE_URL
                path(CONVERSATIONS_PATH, conversationId, MESSAGES_PATH)
            }
            setBody(request)
        }

    override suspend fun uploadAttachment(
        attachment: ChatAttachment,
        type: String,
    ): UploadResponseDto {
        val url = "https://$CHAT_BASE_URL/$UPLOAD_PATH"
        val response =
            httpClient.submitFormWithBinaryData(
                url = url,
                formData =
                    formData {
                        append("type", type)
                        append(
                            key = "file",
                            value =
                                InputProvider(size = attachment.size) {
                                    attachment.openSource()
                                },
                            headers =
                                Headers.build {
                                    append(HttpHeaders.ContentType, attachment.contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${attachment.fileName}\"")
                                },
                        )
                    },
            ) {
                // server expects multipart; keep default request headers + force multipart
                contentType(ContentType.MultiPart.FormData)
            }
        val deserializer = json.serializersModule.serializer<UploadResponseDto>()
        return json.decodeFromString(
            deserializer = deserializer,
            string = response.bodyAsText(),
        )
    }

    private companion object {
        private const val INFLUENCERS_PATH = "api/v1/influencers"
        private const val CONVERSATIONS_PATH = "api/v1/chat/conversations"
        private const val MESSAGES_PATH = "messages"
        private const val UPLOAD_PATH = "api/v1/upload"
    }
}
