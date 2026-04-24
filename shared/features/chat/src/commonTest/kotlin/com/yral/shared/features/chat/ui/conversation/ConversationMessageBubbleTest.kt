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

    @Test
    fun `chat media container height uses default aspect ratio for unknown image ratio`() {
        val result =
            resolveChatMediaContainerSize(
                maxWidthPx = 320f,
                imageAspectRatio = null,
                minHeightPx = 160f,
                maxHeightPx = 360f,
            )

        assertEquals(320, result.widthPx)
        assertEquals(240, result.heightPx)
    }

    @Test
    fun `chat media container size caps tall images and shrinks width`() {
        val result =
            resolveChatMediaContainerSize(
                maxWidthPx = 320f,
                imageAspectRatio = 0.5f,
                minHeightPx = 160f,
                maxHeightPx = 360f,
            )

        assertEquals(180, result.widthPx)
        assertEquals(360, result.heightPx)
    }

    @Test
    fun `chat media container size floors wide images without exceeding max width`() {
        val result =
            resolveChatMediaContainerSize(
                maxWidthPx = 320f,
                imageAspectRatio = 4f,
                minHeightPx = 160f,
                maxHeightPx = 360f,
            )

        assertEquals(320, result.widthPx)
        assertEquals(80, result.heightPx)
    }

    @Test
    fun `chat media aspect ratio returns null for invalid dimensions`() {
        assertNull(resolveChatMediaAspectRatio(imageWidthPx = 0f, imageHeightPx = 200f))
    }
}
