package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.data.models.ConversationDto
import com.yral.shared.features.chat.data.models.ConversationInfluencerDto
import com.yral.shared.features.chat.data.models.ConversationsResponseDto
import com.yral.shared.features.chat.data.models.DeleteConversationResponseDto
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageResponseDto
import com.yral.shared.features.chat.data.models.UploadResponseDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatRepositoryImplUnreadCountTest {
    @Test
    fun `get unread conversation count sums only badge eligible unread counts across pages`() =
        runTest {
            val dataSource =
                FakeChatDataSource(
                    pages =
                        listOf(
                            ConversationsResponseDto(
                                conversations =
                                    listOf(
                                        createConversationDto(id = "conversation-1", unreadCount = 4),
                                        createConversationDto(
                                            id = "conversation-2",
                                            unreadCount = 9,
                                            hasConversationUser = true,
                                        ),
                                    ),
                                total = 4,
                                limit = 2,
                                offset = 0,
                            ),
                            ConversationsResponseDto(
                                conversations =
                                    listOf(
                                        createConversationDto(id = "conversation-3", unreadCount = 0),
                                        createConversationDto(id = "conversation-4", unreadCount = 7),
                                    ),
                                total = 4,
                                limit = 2,
                                offset = 2,
                            ),
                        ),
                )

            val repository = ChatRepositoryImpl(dataSource = dataSource)

            val unreadCount = repository.getUnreadConversationCount(principal = "principal-1")

            assertEquals(11, unreadCount)
            assertEquals(listOf(0, 2), dataSource.requestedOffsets)
        }

    private fun createConversationDto(
        id: String,
        unreadCount: Int,
        hasConversationUser: Boolean = false,
    ): ConversationDto =
        ConversationDto(
            id = id,
            userId = "user-1",
            influencerId = "influencer-1",
            influencer =
                ConversationInfluencerDto(
                    id = "influencer-1",
                    name = "Influencer",
                    displayName = "Influencer",
                    avatarUrl = "https://example.com/avatar.png",
                    category = "fashion",
                    suggestedMessages = emptyList(),
                ),
            user =
                if (hasConversationUser) {
                    com.yral.shared.features.chat.data.models.ConversationUserDto(
                        principalId = "principal-2",
                        username = "bot-user",
                        profilePictureUrl = "https://example.com/bot.png",
                    )
                } else {
                    null
                },
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            messageCount = 1,
            lastMessage = null,
            recentMessages = emptyList(),
            unreadCount = unreadCount,
        )

    private class FakeChatDataSource(
        private val pages: List<ConversationsResponseDto>,
    ) : ChatDataSource {
        val requestedOffsets = mutableListOf<Int>()

        override suspend fun listConversations(
            limit: Int,
            offset: Int,
            influencerId: String?,
            principal: String,
        ): ConversationsResponseDto {
            requestedOffsets += offset
            return pages.first { it.offset == offset }
        }

        override suspend fun listInfluencers(
            limit: Int,
            offset: Int,
        ): InfluencersResponseDto = error("unused")

        override suspend fun listTrendingInfluencers(
            limit: Int,
            offset: Int,
        ): InfluencersResponseDto = error("unused")

        override suspend fun getInfluencer(id: String): InfluencerDto = error("unused")

        override suspend fun createConversation(influencerId: String): ConversationDto = error("unused")

        override suspend fun deleteConversation(conversationId: String): DeleteConversationResponseDto = error("unused")

        override suspend fun listConversationMessages(
            conversationId: String,
            limit: Int,
            offset: Int,
            order: String,
        ) = error("unused")

        override suspend fun sendMessageJson(
            conversationId: String,
            request: SendMessageRequestDto,
        ): SendMessageResponseDto = error("unused")

        override suspend fun uploadAttachment(
            attachment: ChatAttachment,
            type: String,
        ): UploadResponseDto = error("unused")

        override suspend fun markConversationAsRead(conversationId: String) = error("unused")
    }
}
