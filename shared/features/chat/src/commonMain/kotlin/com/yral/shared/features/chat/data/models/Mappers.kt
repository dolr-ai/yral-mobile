package com.yral.shared.features.chat.data.models

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.features.chat.domain.models.ConversationLastMessage
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.ConversationMessagesPageResult
import com.yral.shared.features.chat.domain.models.ConversationUser
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.DeleteConversationResult
import com.yral.shared.features.chat.domain.models.HUMAN_CHAT_CONVERSATION_TYPE
import com.yral.shared.features.chat.domain.models.HumanCreatorTakeoverStatus
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencerStatus
import com.yral.shared.features.chat.domain.models.InfluencersPageResult
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.features.chat.domain.models.SoulFile
import com.yral.shared.features.chat.domain.models.SoulFileSection
import com.yral.shared.rust.service.utils.propicFromPrincipal

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
        messageCount = messageCount,
    )

fun InfluencerFeedResponseDto.toInfluencersResponseDto(): InfluencersResponseDto =
    InfluencersResponseDto(
        influencers =
            influencers.map { influencer ->
                InfluencerDto(
                    id = influencer.id,
                    name = influencer.name,
                    displayName = influencer.displayName,
                    avatarUrl = influencer.avatarUrl,
                    description = influencer.description,
                    category = influencer.category,
                    isActive = InfluencerStatus.ACTIVE.value,
                    createdAt = influencer.createdAt,
                    conversationCount = influencer.signals?.conversationCount,
                    messageCount = influencer.signals?.messageCount,
                )
            },
        total = totalCount,
        limit = limit,
        offset = offset,
        hasMore = hasMore,
    )

fun InfluencersResponseDto.toDomainActiveOnly(): InfluencersPageResult {
    val rawCount = influencers.size
    val activeInfluencers =
        influencers
            .filter { it.isActive in listOf(InfluencerStatus.ACTIVE.value, InfluencerStatus.COMING_SOON.value) }
            .map { it.toDomain() }
    val nextOffset =
        when {
            rawCount == 0 -> null
            hasMore != null -> if (hasMore) offset + rawCount else null
            offset + rawCount < total -> offset + rawCount
            else -> null
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

fun ConversationDto.toDomain(): Conversation {
    val resolvedInfluencer: ConversationInfluencer? =
        influencer?.let {
            ConversationInfluencer(
                id = it.id,
                name = it.name,
                displayName = it.displayName,
                avatarUrl =
                    it.avatarUrl
                        .takeIf { url -> url.isNotEmpty() }
                        ?: propicFromPrincipal(it.id),
                category = it.category.orEmpty(),
                suggestedMessages = it.suggestedMessages.orEmpty(),
            )
        } ?: influencerId?.let { influencerId ->
            ConversationInfluencer(
                id = influencerId,
                name = "",
                displayName = "",
                avatarUrl = "",
            )
        }
    // H2H conversations legitimately carry no influencer. AI / takeover
    // conversations must — keep the original invariant in place for them
    // so a backend bug that drops the influencer doesn't silently
    // degrade the AI chat experience.
    if (resolvedInfluencer == null && conversationType != HUMAN_CHAT_CONVERSATION_TYPE) {
        throw YralException("Conversation requires a valid influencer")
    }
    return Conversation(
        id = id,
        userId = userId,
        influencer = resolvedInfluencer,
        conversationUser =
            user?.let {
                ConversationUser(
                    principalId = it.principalId,
                    username =
                        it.username
                            .takeIf { name -> name?.isNotEmpty() == true }
                            ?: resolveUsername("", it.principalId),
                    profilePictureUrl =
                        it.profilePictureUrl
                            .takeIf { url -> url?.isNotEmpty() == true }
                            ?: propicFromPrincipal(it.principalId),
                )
            },
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
        unreadCount = unreadCount,
        conversationType = conversationType,
    )
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
        senderId = senderId,
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

fun HumanCreatorTakeoverStatusDto.toDomain(): HumanCreatorTakeoverStatus =
    HumanCreatorTakeoverStatus(
        active = active,
        startedAt = startedAt,
        userLastMessageAt = userLastMessageAt,
        remainingSeconds = remainingSeconds,
    )

fun StartHumanCreatorTakeoverResponseDto.toDomain(): HumanCreatorTakeoverStatus =
    HumanCreatorTakeoverStatus(
        active = status.equals("active", ignoreCase = true),
        startedAt = startedAt,
        userLastMessageAt = userLastMessageAt,
        remainingSeconds = remainingSeconds,
    )

// ---------- Coach pivot Bucket 2 — Soul File ----------

fun SoulFileResponseDto.toDomain(): SoulFile =
    SoulFile(
        botId = botId,
        displayName = displayName,
        sections = sections.map { it.toDomain() },
        sectionsVersionSha256 = sectionsVersionSha256,
        fallbackToFlat = fallbackToFlat,
    )

fun SoulFileSectionDto.toDomain(): SoulFileSection =
    SoulFileSection(
        id = id,
        heading = heading,
        body = body,
        editable = editable,
    )

fun SoulFileSection.toDto(): SoulFileSectionDto =
    SoulFileSectionDto(
        id = id,
        heading = heading,
        body = body,
        editable = editable,
    )
