package com.yral.shared.features.chat.ui.conversation

import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.viewmodel.ConversationMessageItem
import com.yral.shared.features.chat.viewmodel.LocalMessage
import com.yral.shared.features.chat.viewmodel.LocalMessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageKeyUtilsTest {
    // region --- Test data factories ---
    private fun createRemoteItem(id: String = "remote-1"): ConversationMessageItem.Remote =
        ConversationMessageItem.Remote(
            message =
                ChatMessage(
                    id = id,
                    conversationId = "conv-1",
                    role = ConversationMessageRole.USER,
                    content = "Hello",
                    messageType = ChatMessageType.TEXT,
                    mediaUrls = emptyList(),
                    audioUrl = null,
                    audioDurationSeconds = null,
                    tokenCount = null,
                    createdAt = "2026-01-01T00:00:00Z",
                ),
        )

    private fun createLocalItem(
        localId: String = "local-1",
        status: LocalMessageStatus = LocalMessageStatus.SENDING,
    ): ConversationMessageItem.Local =
        ConversationMessageItem.Local(
            message =
                LocalMessage(
                    localId = localId,
                    role = ConversationMessageRole.USER,
                    content = "Hello",
                    messageType = ChatMessageType.TEXT,
                    mediaUrls = emptyList(),
                    createdAtMs = 1000L,
                    status = status,
                    isPlaceholder = false,
                    draftForRetry = null,
                ),
        )

    // endregion

    // region --- overlayItemKey ---

    @Test
    fun `overlayItemKey returns local prefix for local items`() {
        val item = createLocalItem(localId = "abc-123")
        assertEquals("local-abc-123", overlayItemKey(item))
    }

    @Test
    fun `overlayItemKey returns remote prefix for remote items`() {
        val item = createRemoteItem(id = "xyz-456")
        assertEquals("remote-xyz-456", overlayItemKey(item))
    }

    @Test
    fun `overlayItemKey produces distinct keys for different local items`() {
        val key1 = overlayItemKey(createLocalItem(localId = "a"))
        val key2 = overlayItemKey(createLocalItem(localId = "b"))
        assertTrue(key1 != key2, "Keys for different local items must differ")
    }

    @Test
    fun `overlayItemKey produces distinct keys for different remote items`() {
        val key1 = overlayItemKey(createRemoteItem(id = "a"))
        val key2 = overlayItemKey(createRemoteItem(id = "b"))
        assertTrue(key1 != key2, "Keys for different remote items must differ")
    }

    @Test
    fun `overlayItemKey produces distinct keys for local and remote items with same id`() {
        val localKey = overlayItemKey(createLocalItem(localId = "same-id"))
        val remoteKey = overlayItemKey(createRemoteItem(id = "same-id"))
        assertTrue(localKey != remoteKey, "Local and remote keys with same ID must differ due to prefix")
    }

    // endregion

    // region --- historyItemKey ---

    @Test
    fun `historyItemKey returns history-local prefix for local items`() {
        val item = createLocalItem(localId = "abc-123")
        assertEquals("history-local-abc-123", historyItemKey(item, 0))
    }

    @Test
    fun `historyItemKey returns history-remote prefix for remote items`() {
        val item = createRemoteItem(id = "xyz-456")
        assertEquals("history-remote-xyz-456", historyItemKey(item, 0))
    }

    @Test
    fun `historyItemKey returns placeholder key when item is null`() {
        assertEquals("history-placeholder-0", historyItemKey(null, 0))
        assertEquals("history-placeholder-5", historyItemKey(null, 5))
    }

    @Test
    fun `historyItemKey ignores index when item is non-null`() {
        val item = createRemoteItem(id = "msg-1")
        assertEquals(historyItemKey(item, 0), historyItemKey(item, 99))
    }

    @Test
    fun `historyItemKey produces unique placeholder keys for different indices`() {
        val key1 = historyItemKey(null, 0)
        val key2 = historyItemKey(null, 1)
        assertTrue(key1 != key2, "Placeholder keys at different indices must differ")
    }

    // endregion

    // region --- overlay vs history key namespace separation ---

    @Test
    fun `overlay and history keys never collide for same local item`() {
        val item = createLocalItem(localId = "shared-id")
        val overlayKey = overlayItemKey(item)
        val historyKey = historyItemKey(item, 0)
        assertTrue(
            overlayKey != historyKey,
            "Overlay key '$overlayKey' and history key '$historyKey' must not collide",
        )
    }

    @Test
    fun `overlay and history keys never collide for same remote item`() {
        val item = createRemoteItem(id = "shared-id")
        val overlayKey = overlayItemKey(item)
        val historyKey = historyItemKey(item, 0)
        assertTrue(
            overlayKey != historyKey,
            "Overlay key '$overlayKey' and history key '$historyKey' must not collide",
        )
    }

    @Test
    fun `all keys are unique across a mixed overlay and history list`() {
        val overlayItems =
            listOf(
                createLocalItem(localId = "local-a"),
                createLocalItem(localId = "local-b"),
                createRemoteItem(id = "remote-a"),
            )
        val historyItems: List<ConversationMessageItem?> =
            listOf(
                createRemoteItem(id = "remote-b"),
                createRemoteItem(id = "remote-c"),
                createLocalItem(localId = "local-c"),
                null, // placeholder
            )

        val allKeys = mutableListOf<String>()
        overlayItems.forEach { allKeys.add(overlayItemKey(it)) }
        historyItems.forEachIndexed { idx, item -> allKeys.add(historyItemKey(item, idx)) }

        assertEquals(
            allKeys.size,
            allKeys.toSet().size,
            "All keys must be unique. Duplicates: ${allKeys.groupBy { it }.filter { it.value.size > 1 }.keys}",
        )
    }

    // endregion

    // region --- messageId ---

    @Test
    fun `messageId returns localId for local items`() {
        val item = createLocalItem(localId = "my-local-id")
        assertEquals("my-local-id", item.messageId())
    }

    @Test
    fun `messageId returns id for remote items`() {
        val item = createRemoteItem(id = "my-remote-id")
        assertEquals("my-remote-id", item.messageId())
    }

    // endregion

    // region --- overlayMessageIdSet ---

    @Test
    fun `overlayMessageIdSet returns empty set for empty list`() {
        val result = overlayMessageIdSet(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `overlayMessageIdSet collects all IDs from mixed items`() {
        val items =
            listOf(
                createLocalItem(localId = "local-1"),
                createRemoteItem(id = "remote-1"),
                createLocalItem(localId = "local-2"),
            )
        val ids = overlayMessageIdSet(items)
        assertEquals(setOf("local-1", "remote-1", "local-2"), ids)
    }

    @Test
    fun `overlayMessageIdSet deduplicates same IDs`() {
        val items =
            listOf(
                createLocalItem(localId = "dup-id"),
                createLocalItem(localId = "dup-id"),
            )
        val ids = overlayMessageIdSet(items)
        assertEquals(1, ids.size)
        assertTrue(ids.contains("dup-id"))
    }

    // endregion

    // region --- isDuplicateOfOverlay ---

    @Test
    fun `isDuplicateOfOverlay returns true when item ID exists in overlay set`() {
        val overlayIds = setOf("msg-1", "msg-2")
        val item = createRemoteItem(id = "msg-1")
        assertTrue(isDuplicateOfOverlay(item, overlayIds))
    }

    @Test
    fun `isDuplicateOfOverlay returns false when item ID not in overlay set`() {
        val overlayIds = setOf("msg-1", "msg-2")
        val item = createRemoteItem(id = "msg-3")
        assertFalse(isDuplicateOfOverlay(item, overlayIds))
    }

    @Test
    fun `isDuplicateOfOverlay works for local items`() {
        val overlayIds = setOf("local-abc")
        val item = createLocalItem(localId = "local-abc")
        assertTrue(isDuplicateOfOverlay(item, overlayIds))
    }

    @Test
    fun `isDuplicateOfOverlay returns false for empty overlay set`() {
        val item = createRemoteItem(id = "msg-1")
        assertFalse(isDuplicateOfOverlay(item, emptySet()))
    }

    // endregion

    // region --- Integration: simulate full key generation scenario ---

    @Test
    fun `full scenario - overlapping items produce unique keys and duplicates are detected`() {
        // Overlay has local-1 (sending) and its placeholder assistant response
        val overlayItems =
            listOf(
                createLocalItem(localId = "user-msg-1"),
                createLocalItem(localId = "assistant-placeholder", status = LocalMessageStatus.WAITING),
            )

        // History has user-msg-1 (already synced) and other older messages
        val historyItems: List<ConversationMessageItem?> =
            listOf(
                createRemoteItem(id = "user-msg-1"), // duplicate of overlay
                createRemoteItem(id = "old-msg-1"),
                createRemoteItem(id = "old-msg-2"),
            )

        // Step 1: Verify all keys are unique
        val allKeys = mutableListOf<String>()
        overlayItems.forEach { allKeys.add(overlayItemKey(it)) }
        historyItems.forEachIndexed { idx, item -> allKeys.add(historyItemKey(item, idx)) }
        assertEquals(
            allKeys.size,
            allKeys.toSet().size,
            "All keys must be unique even with overlapping message IDs",
        )

        // Step 2: Verify duplicate detection filters the synced message
        val overlayIds = overlayMessageIdSet(overlayItems)
        val renderedHistoryItems = historyItems.filterNotNull().filterNot { isDuplicateOfOverlay(it, overlayIds) }

        // user-msg-1 should be filtered out, leaving only old-msg-1 and old-msg-2
        assertEquals(2, renderedHistoryItems.size)
        assertEquals("old-msg-1", renderedHistoryItems[0].messageId())
        assertEquals("old-msg-2", renderedHistoryItems[1].messageId())
    }

    @Test
    fun `full scenario - no overlap means no items are filtered`() {
        val overlayItems =
            listOf(
                createLocalItem(localId = "new-msg"),
            )
        val historyItems =
            listOf(
                createRemoteItem(id = "old-1"),
                createRemoteItem(id = "old-2"),
            )

        val overlayIds = overlayMessageIdSet(overlayItems)
        val renderedHistoryItems = historyItems.filterNot { isDuplicateOfOverlay(it, overlayIds) }

        assertEquals(2, renderedHistoryItems.size)
    }

    @Test
    fun `full scenario - empty overlay means all history items are rendered`() {
        val historyItems =
            listOf(
                createRemoteItem(id = "msg-1"),
                createRemoteItem(id = "msg-2"),
            )

        val overlayIds = overlayMessageIdSet(emptyList())
        val renderedHistoryItems = historyItems.filterNot { isDuplicateOfOverlay(it, overlayIds) }

        assertEquals(2, renderedHistoryItems.size)
    }

    // endregion
}
