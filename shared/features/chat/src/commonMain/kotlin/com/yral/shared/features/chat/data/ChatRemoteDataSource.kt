package com.yral.shared.features.chat.data

import co.touchlab.kermit.Logger
import com.yral.featureflag.ChatFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.data.models.ChatMessageDto
import com.yral.shared.features.chat.data.models.ConversationDto
import com.yral.shared.features.chat.data.models.ConversationMessagesResponseDto
import com.yral.shared.features.chat.data.models.ConversationsResponseDto
import com.yral.shared.features.chat.data.models.CreateConversationRequestDto
import com.yral.shared.features.chat.data.models.CreateHumanConversationRequestDto
import com.yral.shared.features.chat.data.models.DeleteConversationResponseDto
import com.yral.shared.features.chat.data.models.HumanCreatorTakeoverStatusDto
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencerFeedResponseDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.features.chat.data.models.DiscoverySearchResponseDto
import com.yral.shared.features.chat.data.models.InboxSearchResponseDto
import com.yral.shared.features.chat.data.models.SystemPromptPreviewResponseDto
import com.yral.shared.features.chat.data.models.ReleaseHumanCreatorTakeoverResponseDto
import com.yral.shared.features.chat.data.models.SendHumanCreatorMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageResponseDto
import com.yral.shared.features.chat.data.models.StartHumanCreatorTakeoverResponseDto
import com.yral.shared.features.chat.data.models.UploadResponseDto
import com.yral.shared.features.chat.data.models.toInfluencersResponseDto
import com.yral.shared.http.UPLOAD_FILE_TIME_OUT
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Suppress("TooManyFunctions")
class ChatRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val chatBaseUrl: String,
    private val influencerFeedBaseUrl: String,
    private val featureFlagManager: FeatureFlagManager,
) : ChatDataSource {
    override suspend fun listInfluencers(
        limit: Int,
        offset: Int,
    ): InfluencersResponseDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(INFLUENCERS_PATH)
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    /**
     * Discovery feed fetch. Flag-gated cutover from Anshuman's recsys host
     * to the v2 agent host. The response envelope is byte-compatible
     * (same field names, identical [InfluencerFeedResponseDto] shape),
     * so only the request URL — and JWT attachment when logged in —
     * differs. OFF path stays exactly as it was pre-flag for instant
     * rollback.
     */
    override suspend fun listTrendingInfluencers(
        limit: Int,
        offset: Int,
    ): InfluencersResponseDto {
        val v2Enabled = featureFlagManager.isEnabled(ChatFeatureFlags.Chat.DiscoveryFeedV2Enabled)
        return if (v2Enabled) {
            val idToken = getIdTokenOrNull()
            httpGet<InfluencerFeedResponseDto>(
                httpClient = httpClient,
                json = json,
            ) {
                url {
                    host = chatBaseUrl
                    path(DISCOVERY_FEED_PATH)
                    parameters.append("limit", limit.toString())
                    parameters.append("offset", offset.toString())
                }
                if (!idToken.isNullOrBlank()) {
                    headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
                }
            }.toInfluencersResponseDto()
        } else {
            httpGet<InfluencerFeedResponseDto>(
                httpClient = httpClient,
                json = json,
            ) {
                url {
                    host = influencerFeedBaseUrl
                    path(INFLUENCER_FEED_PATH)
                    parameters.append("limit", limit.toString())
                    parameters.append("offset", offset.toString())
                }
            }.toInfluencersResponseDto()
        }
    }

    /**
     * Discovery search — `GET /api/v2/discovery/search?q=&limit=`. Hits
     * the v2 host with the user's JWT when logged in (server falls back
     * to non-personalised ranking when absent). The feature flag is
     * enforced upstream in the repository — this method assumes the
     * caller already confirmed `DiscoverySearchEnabled` is ON.
     */
    override suspend fun searchDiscovery(
        query: String,
        limit: Int,
    ): DiscoverySearchResponseDto {
        val idToken = getIdTokenOrNull()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(DISCOVERY_SEARCH_PATH)
                parameters.append("q", query)
                parameters.append("limit", limit.toString())
            }
            if (!idToken.isNullOrBlank()) {
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
        }
    }

    /**
     * Inbox conversation search — `GET /api/v2/chat/conversations/search?q=&limit=`.
     * Owner-scoped (requires JWT — anonymous callers get an empty list
     * server-side). Mirrors the discovery search wire path but talks to
     * a different router that only sees the calling user's conversations.
     */
    override suspend fun searchInbox(
        query: String,
        limit: Int,
    ): InboxSearchResponseDto {
        val idToken = getIdTokenOrNull()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(INBOX_SEARCH_PATH)
                parameters.append("q", query)
                parameters.append("limit", limit.toString())
            }
            if (!idToken.isNullOrBlank()) {
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
        }
    }

    override suspend fun getInfluencer(id: String): InfluencerDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(INFLUENCERS_PATH, id)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    /**
     * Coach pivot Bucket 2 — `GET /api/v1/influencers/{botId}/system-prompt-preview`.
     * Owner-gated. Returns the FULL composed system prompt the LLM sees at
     * chat time (L1–L4 layers + skills + applied overrides) so the creator
     * can audit it from the read-only "View full prompt" page in Coach.
     *
     * Backend ships `Cache-Control: no-store`. Mobile re-fetches on every
     * page open (the ViewModel calls `load()` on each `openForBot`); we do
     * not hold this in any local cache.
     */
    override suspend fun getSystemPromptPreview(botId: String): SystemPromptPreviewResponseDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(INFLUENCERS_PATH, botId, SYSTEM_PROMPT_PREVIEW_SUBPATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun createConversation(influencerId: String): ConversationDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CONVERSATIONS_PATH)
            }
            setBody(
                CreateConversationRequestDto(
                    influencerId = influencerId,
                ),
            )
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun createHumanConversation(participantId: String): ConversationDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(HUMAN_CONVERSATIONS_PATH)
            }
            setBody(
                CreateHumanConversationRequestDto(
                    participantId = participantId,
                ),
            )
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun sendHumanMessage(
        conversationId: String,
        request: SendMessageRequestDto,
    ): SendMessageResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(HUMAN_CONVERSATIONS_PATH, conversationId, MESSAGES_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            setBody(request)
        }
    }

    override suspend fun listConversations(
        limit: Int,
        offset: Int,
        influencerId: String?,
        principal: String,
    ): ConversationsResponseDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CONVERSATIONS_LIST_PATH)
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
                parameters.append("principal", principal)
                if (!influencerId.isNullOrBlank()) {
                    parameters.append("influencer_id", influencerId)
                }
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun deleteConversation(conversationId: String): DeleteConversationResponseDto {
        val idToken = getIdToken()
        val response =
            httpClient.delete {
                url {
                    host = chatBaseUrl
                    path(CONVERSATIONS_PATH, conversationId)
                }
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
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
    ): ConversationMessagesResponseDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CONVERSATIONS_PATH, conversationId, MESSAGES_PATH)
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
                parameters.append("order", order)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun sendMessageJson(
        conversationId: String,
        request: SendMessageRequestDto,
    ): SendMessageResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CONVERSATIONS_PATH, conversationId, MESSAGES_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            setBody(request)
        }
    }

    override suspend fun uploadAttachment(
        attachment: ChatAttachment,
        type: String,
    ): UploadResponseDto {
        val idToken = getIdToken()
        val attachmentBytes = attachment.readUploadBytes()
        val url =
            URLBuilder("https://$chatBaseUrl")
                .apply {
                    appendPathSegments(UPLOAD_PATH.split('/'))
                }.build()
                .toString()
        logger.d {
            "Uploading chat attachment file=${attachment.fileName} " +
                "size=${attachmentBytes.size} contentType=${attachment.contentType} type=$type"
        }
        val response =
            httpClient.submitFormWithBinaryData(
                url = url,
                formData =
                    formData {
                        append("type", type)
                        append(
                            key = "file",
                            value = attachmentBytes,
                            headers =
                                Headers.build {
                                    append(HttpHeaders.ContentType, attachment.contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${attachment.fileName}\"")
                                },
                        )
                    },
            ) {
                expectSuccess = false
                timeout {
                    connectTimeoutMillis = UPLOAD_FILE_TIME_OUT
                    requestTimeoutMillis = UPLOAD_FILE_TIME_OUT
                    socketTimeoutMillis = UPLOAD_FILE_TIME_OUT
                }
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
        val responseBody = runCatching { response.bodyAsText() }.getOrDefault("")
        logger.d {
            "Chat attachment upload completed status=${response.status.value} file=${attachment.fileName}"
        }
        if (!response.status.isSuccess()) {
            logger.e {
                "Chat attachment upload failed status=${response.status.value} file=${attachment.fileName}"
            }
            throw YralException("Chat attachment upload failed (${response.status.value}): $responseBody")
        }
        val deserializer = json.serializersModule.serializer<UploadResponseDto>()
        return runCatching {
            json.decodeFromString(
                deserializer = deserializer,
                string = responseBody,
            )
        }.getOrElse { error ->
            logger.e(error) {
                "Failed to decode chat attachment upload response status=${response.status.value} " +
                    "file=${attachment.fileName}"
            }
            throw YralException("Chat attachment upload decode failed", error)
        }
    }

    override suspend fun markConversationAsRead(conversationId: String) {
        val idToken = getIdToken()
        httpClient.post {
            url {
                host = chatBaseUrl
                path(CONVERSATIONS_PATH, conversationId, READ_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun startHumanCreatorTakeover(conversationId: String): StartHumanCreatorTakeoverResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CREATOR_CONVERSATIONS_PATH, conversationId, TAKEOVER_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun releaseHumanCreatorTakeover(conversationId: String): ReleaseHumanCreatorTakeoverResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CREATOR_CONVERSATIONS_PATH, conversationId, RELEASE_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun sendHumanCreatorMessage(
        conversationId: String,
        request: SendHumanCreatorMessageRequestDto,
    ): ChatMessageDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CREATOR_CONVERSATIONS_PATH, conversationId, CREATOR_MESSAGES_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            setBody(request)
        }
    }

    override suspend fun getHumanCreatorTakeoverStatus(conversationId: String): HumanCreatorTakeoverStatusDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CREATOR_CONVERSATIONS_PATH, conversationId, TAKEOVER_STATUS_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun listCreatorConversationMessages(
        conversationId: String,
        limit: Int,
        offset: Int,
        order: String,
    ): ConversationMessagesResponseDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(CREATOR_CONVERSATIONS_PATH, conversationId, MESSAGES_PATH)
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
                parameters.append("order", order)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    private suspend fun getIdToken() =
        preferences.getString(PrefKeys.ID_TOKEN.name)
            ?: throw YralException("Authorisation not found")

    /**
     * Discovery feed v2 is owner-optional: backend personalises the feed
     * when a JWT is present and falls back to a generic ranking when it
     * isn't, so the call site needs to suppress the throw on missing
     * token rather than treat it as an auth error.
     */
    private suspend fun getIdTokenOrNull() =
        preferences.getString(PrefKeys.ID_TOKEN.name)

    private fun ChatAttachment.readUploadBytes(): ByteArray =
        when (this) {
            is FilePathChatAttachment -> readChatAttachmentBytes(filePath)
            else -> throw YralException("Chat attachment upload requires file-backed attachment")
        }

    private companion object {
        private val logger = Logger.withTag("ChatRemoteDataSource")
        private const val INFLUENCERS_PATH = "api/v1/influencers"
        private const val INFLUENCER_FEED_PATH = "api/v1/influencer-feed"
        private const val DISCOVERY_FEED_PATH = "api/v2/discovery/influencer-feed"
        private const val DISCOVERY_SEARCH_PATH = "api/v2/discovery/search"
        private const val INBOX_SEARCH_PATH = "api/v2/chat/conversations/search"
        private const val SYSTEM_PROMPT_PREVIEW_SUBPATH = "system-prompt-preview"
        private const val CONVERSATIONS_PATH = "api/v1/chat/conversations"
        private const val HUMAN_CONVERSATIONS_PATH = "api/v1/chat/human/conversations"
        private const val CONVERSATIONS_LIST_PATH = "api/v2/chat/conversations"
        private const val MESSAGES_PATH = "messages"
        private const val READ_PATH = "read"
        private const val UPLOAD_PATH = "api/v1/media/upload"
        private const val CREATOR_CONVERSATIONS_PATH = "api/v1/creator/conversations"
        private const val TAKEOVER_PATH = "human-creator-takeover"
        private const val RELEASE_PATH = "human-creator-release"
        private const val CREATOR_MESSAGES_PATH = "human-creator-messages"
        private const val TAKEOVER_STATUS_PATH = "human-creator-takeover-status"
    }
}
