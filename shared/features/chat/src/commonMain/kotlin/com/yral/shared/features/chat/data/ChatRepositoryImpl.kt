package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.toDomain
import com.yral.shared.features.chat.data.models.toDomainActiveOnly
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationMessagesPageResult
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.DeleteConversationResult
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencersPageResult
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult

class ChatRepositoryImpl(
    private val dataSource: ChatDataSource,
) : ChatRepository {
    override suspend fun getInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult =
        dataSource
            .listInfluencers(limit = limit, offset = offset)
            .toDomainActiveOnly()

    override suspend fun getInfluencer(id: String): Influencer =
        dataSource
            .getInfluencer(id)
            .toDomain()

    override suspend fun createConversation(influencerId: String): Conversation =
        dataSource
            .createConversation(influencerId)
            .toDomain()

    override suspend fun getConversationsPage(
        limit: Int,
        offset: Int,
        influencerId: String?,
    ): ConversationsPageResult =
        dataSource
            .listConversations(
                limit = limit,
                offset = offset,
                influencerId = influencerId,
            ).toDomain()

    override suspend fun deleteConversation(conversationId: String): DeleteConversationResult =
        dataSource
            .deleteConversation(conversationId)
            .toDomain()

    override suspend fun getConversationMessagesPage(
        conversationId: String,
        limit: Int,
        offset: Int,
    ): ConversationMessagesPageResult =
        dataSource
            .listConversationMessages(
                conversationId = conversationId,
                limit = limit,
                offset = offset,
                order = "desc",
            ).toDomain()

    override suspend fun sendMessage(
        conversationId: String,
        draft: SendMessageDraft,
    ): SendMessageResult {
        val mediaUrls =
            if (draft.mediaAttachments.isNotEmpty()) {
                draft.mediaAttachments.map { attachment ->
                    dataSource
                        .uploadAttachment(
                            attachment = attachment,
                            type = ChatMessageType.IMAGE.apiValue,
                        ).storageKey
                }
            } else {
                null
            }

        val audioUrl =
            draft.audioAttachment?.let { attachment ->
                dataSource
                    .uploadAttachment(
                        attachment = attachment,
                        type = ChatMessageType.AUDIO.apiValue,
                    ).storageKey
            }

        val response =
            dataSource.sendMessageJson(
                conversationId = conversationId,
                request =
                    SendMessageRequestDto(
                        content = draft.content,
                        messageType = draft.messageType.apiValue,
                        mediaUrls = mediaUrls,
                        audioUrl = audioUrl,
                        audioDurationSeconds = draft.audioDurationSeconds,
                    ),
            )
        return response.toDomain(conversationIdFallback = conversationId)
    }
}
