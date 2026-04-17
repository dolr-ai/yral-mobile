package com.yral.shared.features.chat.ui.conversation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConversationMessageBubbleTest {
    @Test
    fun `local chat image file path returns bare local path`() {
        val localPath = "/var/mobile/Containers/Data/Application/chat_image_1.jpg"

        assertEquals(localPath, localChatImageFilePathOrNull(localPath))
    }

    @Test
    fun `local chat image file path strips file scheme`() {
        assertEquals(
            "/var/mobile/Containers/Data/Application/chat_image_2.jpg",
            localChatImageFilePathOrNull("file:///var/mobile/Containers/Data/Application/chat_image_2.jpg"),
        )
    }

    @Test
    fun `remote chat image url is not treated as local file`() {
        assertNull(localChatImageFilePathOrNull("https://cdn.yral.com/chat/chat_image.jpg"))
    }
}
