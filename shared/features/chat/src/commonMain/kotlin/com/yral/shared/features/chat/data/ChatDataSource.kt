package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.data.models.ChatMessageDto
import com.yral.shared.features.chat.data.models.ConversationDto
import com.yral.shared.features.chat.data.models.ConversationMessagesResponseDto
import com.yral.shared.features.chat.data.models.ConversationsResponseDto
import com.yral.shared.features.chat.data.models.DeleteConversationResponseDto
import com.yral.shared.features.chat.data.models.HumanCreatorTakeoverStatusDto
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.DiscoverySearchResponseDto
import com.yral.shared.features.chat.data.models.SystemPromptPreviewResponseDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.features.chat.data.models.ReleaseHumanCreatorTakeoverResponseDto
import com.yral.shared.features.chat.data.models.SendHumanCreatorMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageResponseDto
import com.yral.shared.features.chat.data.models.StartHumanCreatorTakeoverResponseDto
import com.yral.shared.features.chat.data.models.UploadResponseDto

interface ChatDataSource {
    suspend fun listInfluencers(
        limit: Int,
        offset: Int,
    ): InfluencersResponseDto

    suspend fun listTrendingInfluencers(
        limit: Int,
        offset: Int,
    ): InfluencersResponseDto

    suspend fun searchDiscovery(
        query: String,
        limit: Int,
    ): DiscoverySearchResponseDto

    suspend fun getInfluencer(id: String): InfluencerDto

    // ---------- Coach pivot Bucket 2 — View full prompt page ----------

    suspend fun getSystemPromptPreview(botId: String): SystemPromptPreviewResponseDto

    suspend fun createConversation(influencerId: String): ConversationDto

    /**
     * H2H: idempotent create-or-fetch of a 1:1 human conversation with
     * [participantId]. POST /api/v1/chat/human/conversations.
     */
    suspend fun createHumanConversation(participantId: String): ConversationDto

    /**
     * H2H: post a message to the human conversation [conversationId].
     * POST /api/v1/chat/human/conversations/{id}/messages.
     *
     * Reuses [SendMessageRequestDto] — the backend ignores
     * `is_streaming` / `is_safety_fallback` for H2H, and the existing
     * non-null body fields (content, message_type, media_urls, audio_url,
     * audio_duration_seconds) are identical between AI and H2H sends.
     */
    suspend fun sendHumanMessage(
        conversationId: String,
        request: SendMessageRequestDto,
    ): SendMessageResponseDto

    suspend fun listConversations(
        limit: Int,
        offset: Int,
        influencerId: String? = null,
        principal: String,
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

    suspend fun markConversationAsRead(conversationId: String)

    suspend fun startHumanCreatorTakeover(conversationId: String): StartHumanCreatorTakeoverResponseDto

    suspend fun releaseHumanCreatorTakeover(conversationId: String): ReleaseHumanCreatorTakeoverResponseDto

    suspend fun sendHumanCreatorMessage(
        conversationId: String,
        request: SendHumanCreatorMessageRequestDto,
    ): ChatMessageDto

    suspend fun getHumanCreatorTakeoverStatus(conversationId: String): HumanCreatorTakeoverStatusDto

    suspend fun listCreatorConversationMessages(
        conversationId: String,
        limit: Int,
        offset: Int,
        order: String = "desc",
    ): ConversationMessagesResponseDto
}
