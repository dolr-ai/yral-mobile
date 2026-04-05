package com.yral.shared.features.chat.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnreadConversationCountTest {
    @Test
    fun `conversation counts toward unread badge only when unread and not conversation user`() {
        assertTrue(createConversation(unreadCount = 3).countsTowardUnreadConversationBadge())
        assertFalse(createConversation(unreadCount = 0).countsTowardUnreadConversationBadge())
        assertFalse(
            createConversation(
                unreadCount = 3,
                conversationUser =
                    ConversationUser(
                        principalId = "principal-1",
                        username = "bot-user",
                        profilePictureUrl = null,
                    ),
            ).countsTowardUnreadConversationBadge(),
        )
    }

    @Test
    fun `total unread badge count sums only badge eligible conversations`() {
        val total =
            listOf(
                createConversation(unreadCount = 5),
                createConversation(unreadCount = 0),
                createConversation(unreadCount = 8, conversationUser = createConversationUser()),
                createConversation(unreadCount = 7),
            ).totalUnreadConversationBadgeCount()

        assertEquals(12, total)
    }

    @Test
    fun `format unread badge count caps at ninety nine plus`() {
        assertEquals(null, formatChatUnreadBadgeCount(-1))
        assertEquals(null, formatChatUnreadBadgeCount(0))
        assertEquals("1", formatChatUnreadBadgeCount(1))
        assertEquals("12", formatChatUnreadBadgeCount(12))
        assertEquals("99", formatChatUnreadBadgeCount(99))
        assertEquals("99+", formatChatUnreadBadgeCount(100))
        assertEquals("99+", formatChatUnreadBadgeCount(150))
        assertEquals("99+", formatChatUnreadBadgeCount(Int.MAX_VALUE))
    }

    private fun createConversation(
        unreadCount: Int,
        conversationUser: ConversationUser? = null,
    ): Conversation =
        Conversation(
            id = "conversation-$unreadCount-${conversationUser?.principalId.orEmpty()}",
            userId = "user-1",
            influencer =
                ConversationInfluencer(
                    id = "influencer-1",
                    name = "Influencer",
                    displayName = "Influencer",
                    avatarUrl = "https://example.com/avatar.png",
                ),
            conversationUser = conversationUser,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            messageCount = 1,
            lastMessage = null,
            unreadCount = unreadCount,
        )

    private fun createConversationUser(): ConversationUser =
        ConversationUser(
            principalId = "principal-1",
            username = "bot-user",
            profilePictureUrl = null,
        )
}
