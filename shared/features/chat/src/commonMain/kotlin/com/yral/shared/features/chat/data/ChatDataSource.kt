package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.data.models.ConversationDto
import com.yral.shared.features.chat.data.models.ConversationMessagesResponseDto
import com.yral.shared.features.chat.data.models.ConversationsResponseDto
import com.yral.shared.features.chat.data.models.DeleteConversationResponseDto
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageResponseDto
import com.yral.shared.features.chat.data.models.UploadResponseDto

interface ChatDataSource {
    suspend fun listInfluencers(
        limit: Int,
        offset: Int,
    ): InfluencersResponseDto

    suspend fun getInfluencer(id: String): InfluencerDto

    suspend fun createConversation(influencerId: String): ConversationDto

    suspend fun listConversations(
        limit: Int,
        offset: Int,
        influencerId: String? = null,
    ): ConversationsResponseDto

    suspend fun deleteConversation(conversationId: String): DeleteConversationResponseDto

    suspend fun listConversationMessages(
        conversationId: String,
        limit: Int,
        offset: Int,
        order: String = "desc",
    ): ConversationMessagesResponseDto

    suspend fun sendMessageJson(
        conversationId: String,
        request: SendMessageRequestDto,
    ): SendMessageResponseDto

    suspend fun uploadAttachment(
        attachment: ChatAttachment,
        type: String,
    ): UploadResponseDto
}
