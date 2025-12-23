package com.yral.shared.features.chat.data.models

import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.features.chat.domain.models.ConversationLastMessage
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.ConversationMessagesPageResult
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.DeleteConversationResult
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencerStatus
import com.yral.shared.features.chat.domain.models.InfluencersPageResult
import com.yral.shared.features.chat.domain.models.SendMessageResult

fun InfluencerDto.toDomain(): Influencer =
    Influencer(
        id = id,
        name = name,
        displayName = displayName,
        avatarUrl = avatarUrl,
        description = description,
        category = category,
        status = InfluencerStatus.fromString(isActive),
        createdAt = createdAt,
        conversationCount = conversationCount,
    )

fun InfluencersResponseDto.toDomainActiveOnly(): InfluencersPageResult {
    val rawCount = influencers.size
    val activeInfluencers =
        influencers
            .filter { it.isActive in listOf(InfluencerStatus.ACTIVE.value, InfluencerStatus.COMING_SOON.value) }
            .map { it.toDomain() }
    val nextOffset =
        if (rawCount > 0 && offset + rawCount < total) {
            offset + rawCount
        } else {
            null
        }

    return InfluencersPageResult(
        influencers = activeInfluencers,
        total = total,
        limit = limit,
        offset = offset,
        nextOffset = nextOffset,
        rawCount = rawCount,
    )
}

fun ConversationDto.toDomain(): Conversation =
    Conversation(
        id = id,
        userId = userId,
        influencer =
            ConversationInfluencer(
                id = influencer.id,
                name = influencer.name,
                displayName = influencer.displayName,
                avatarUrl = influencer.avatarUrl,
                suggestedMessages = influencer.suggestedMessages.orEmpty(),
            ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        messageCount = messageCount,
        lastMessage =
            lastMessage?.let {
                ConversationLastMessage(
                    content = it.content,
                    role = ConversationMessageRole.fromApi(it.role),
                    createdAt = it.createdAt,
                )
            },
        recentMessages = recentMessages?.map { it.toDomain(conversationIdFallback = id) } ?: emptyList(),
    )

fun ConversationsResponseDto.toDomain(): ConversationsPageResult {
    val rawCount = conversations.size
    val nextOffset =
        if (rawCount > 0 && offset + rawCount < total) {
            offset + rawCount
        } else {
            null
        }

    return ConversationsPageResult(
        conversations = conversations.map { it.toDomain() },
        total = total,
        limit = limit,
        offset = offset,
        nextOffset = nextOffset,
        rawCount = rawCount,
    )
}

fun DeleteConversationResponseDto.toDomain(): DeleteConversationResult =
    DeleteConversationResult(
        success = success,
        message = message,
        deletedConversationId = deletedConversationId,
        deletedMessagesCount = deletedMessagesCount,
    )

fun ChatMessageDto.toDomain(conversationIdFallback: String? = null): ChatMessage {
    val resolvedConversationId =
        conversationId ?: conversationIdFallback
            ?: error("ChatMessage requires a valid conversationId")
    return ChatMessage(
        id = id,
        conversationId = resolvedConversationId,
        role = ConversationMessageRole.fromApi(role),
        content = content,
        messageType = ChatMessageType.fromApi(messageType),
        mediaUrls = mediaUrls.orEmpty(),
        audioUrl = audioUrl,
        audioDurationSeconds = audioDurationSeconds,
        tokenCount = tokenCount,
        createdAt = createdAt,
    )
}

fun ConversationMessagesResponseDto.toDomain(): ConversationMessagesPageResult {
    val rawCount = messages.size
    val nextOffset =
        if (rawCount > 0 && offset + rawCount < total) {
            offset + rawCount
        } else {
            null
        }

    return ConversationMessagesPageResult(
        conversationId = conversationId,
        messages = messages.map { it.toDomain(conversationIdFallback = conversationId) },
        total = total,
        limit = limit,
        offset = offset,
        nextOffset = nextOffset,
        rawCount = rawCount,
    )
}

fun SendMessageResponseDto.toDomain(conversationIdFallback: String): SendMessageResult =
    SendMessageResult(
        userMessage = userMessage.toDomain(conversationIdFallback),
        assistantMessage = assistantMessage?.toDomain(conversationIdFallback),
    )
