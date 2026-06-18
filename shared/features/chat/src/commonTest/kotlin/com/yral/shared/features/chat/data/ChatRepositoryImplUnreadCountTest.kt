package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.data.models.ChatMessageDto
import com.yral.shared.features.chat.data.models.SystemPromptPreviewResponseDto
import com.yral.shared.features.chat.data.models.ConversationDto
import com.yral.shared.features.chat.data.models.ConversationInfluencerDto
import com.yral.shared.features.chat.data.models.ConversationMessagesResponseDto
import com.yral.shared.features.chat.data.models.ConversationsResponseDto
import com.yral.shared.features.chat.data.models.DeleteConversationResponseDto
import com.yral.shared.features.chat.data.models.HumanCreatorTakeoverStatusDto
import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto
import com.yral.shared.features.chat.data.models.ReleaseHumanCreatorTakeoverResponseDto
import com.yral.shared.features.chat.data.models.SendHumanCreatorMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.SendMessageResponseDto
import com.yral.shared.features.chat.data.models.StartHumanCreatorTakeoverResponseDto
import com.yral.shared.features.chat.data.models.UploadResponseDto
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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

            val repository =
                ChatRepositoryImpl(
                    dataSource = dataSource,
                    streamingDataSource = createUnusedStreamingDataSource(),
                )

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

    private fun createUnusedStreamingDataSource(): ChatStreamingDataSource =
        ChatStreamingDataSource(
            httpClient = HttpClient(),
            json = Json,
            preferences = DummyPreferences,
            chatBaseUrl = "example.com",
        )

    private object DummyPreferences : Preferences {
        override suspend fun putBoolean(
            key: String,
            boolean: Boolean,
        ) = error("unused")

        override suspend fun getBoolean(key: String): Boolean? = error("unused")

        override suspend fun putString(
            key: String,
            value: String,
        ) = error("unused")

        override suspend fun getString(key: String): String? = error("unused")

        override suspend fun putInt(
            key: String,
            int: Int,
        ) = error("unused")

        override suspend fun getInt(key: String): Int? = error("unused")

        override suspend fun putLong(
            key: String,
            long: Long,
        ) = error("unused")

        override suspend fun getLong(key: String): Long? = error("unused")

        override suspend fun putFloat(
            key: String,
            float: Float,
        ) = error("unused")

        override suspend fun getFloat(key: String): Float? = error("unused")

        override suspend fun putDouble(
            key: String,
            double: Double,
        ) = error("unused")

        override suspend fun getDouble(key: String): Double? = error("unused")

        override suspend fun putBytes(
            key: String,
            bytes: ByteArray,
        ) = error("unused")

        override suspend fun getBytes(key: String): ByteArray? = error("unused")

        override suspend fun remove(key: String) = error("unused")

        override suspend fun clearAll() = error("unused")
    }

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

        override suspend fun getSystemPromptPreview(
            botId: String,
        ): SystemPromptPreviewResponseDto = error("unused")

        override suspend fun createConversation(influencerId: String): ConversationDto = error("unused")

        override suspend fun createHumanConversation(participantId: String): ConversationDto = error("unused")

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

        override suspend fun sendHumanMessage(
            conversationId: String,
            request: SendMessageRequestDto,
        ): SendMessageResponseDto = error("unused")

        override suspend fun uploadAttachment(
            attachment: ChatAttachment,
            type: String,
        ): UploadResponseDto = error("unused")

        override suspend fun markConversationAsRead(conversationId: String) = error("unused")

        override suspend fun startHumanCreatorTakeover(conversationId: String): StartHumanCreatorTakeoverResponseDto = error("unused")

        override suspend fun releaseHumanCreatorTakeover(conversationId: String): ReleaseHumanCreatorTakeoverResponseDto = error("unused")

        override suspend fun sendHumanCreatorMessage(
            conversationId: String,
            request: SendHumanCreatorMessageRequestDto,
        ): ChatMessageDto = error("unused")

        override suspend fun getHumanCreatorTakeoverStatus(conversationId: String): HumanCreatorTakeoverStatusDto = error("unused")

        override suspend fun listCreatorConversationMessages(
            conversationId: String,
            limit: Int,
            offset: Int,
            order: String,
        ): ConversationMessagesResponseDto = error("unused")
    }
}
