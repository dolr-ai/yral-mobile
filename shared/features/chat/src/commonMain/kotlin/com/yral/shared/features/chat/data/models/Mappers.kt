package com.yral.shared.features.chat.data.models

import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.features.chat.domain.models.ConversationLastMessage
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencersPageResult

fun InfluencerDto.toDomain(): Influencer =
    Influencer(
        id = id,
        name = name,
        displayName = displayName,
        avatarUrl = avatarUrl,
        description = description,
        category = category,
        isActive = isActive,
        createdAt = createdAt,
        conversationCount = conversationCount,
    )

fun InfluencersResponseDto.toDomainActiveOnly(): InfluencersPageResult {
    val rawCount = influencers.size
    val activeInfluencers = influencers.filter { it.isActive }.map { it.toDomain() }
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
            ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        messageCount = messageCount,
        lastMessage =
            lastMessage?.let {
                ConversationLastMessage(
                    content = it.content,
                    role = it.role.toConversationMessageRole(),
                    createdAt = it.createdAt,
                )
            },
    )

private fun String.toConversationMessageRole(): ConversationMessageRole =
    when (trim().lowercase()) {
        ConversationMessageRole.USER.apiValue -> ConversationMessageRole.USER
        ConversationMessageRole.ASSISTANT.apiValue -> ConversationMessageRole.ASSISTANT
        else -> ConversationMessageRole.USER
    }

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
