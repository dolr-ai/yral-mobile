package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.domain.models.ChatMessage

/**
 * Phase 5b: in-memory LRU cache for the last-rendered message list per conversation,
 * used for stale-while-revalidate on conversation re-entry.
 *
 * On ConversationViewModel construction the cache is read to pre-populate
 * `_overlay.sent` so the user sees their prior chat instantly. The existing paging
 * cycle fetches page 0 in the background; once the paged versions of the same
 * messages arrive their ids enter `loadedMessageIds`, the combine filter removes
 * the cached overlay copies, and the LazyColumn smoothly transitions to paged
 * rendering — no visual change.
 *
 * In-memory only. Survives navigation within the running process; lost on app kill.
 * Disk persistence is a deliberate non-goal for this PR.
 */
class ConversationContentCache(
    private val maxConversations: Int = DEFAULT_MAX_CONVERSATIONS,
    private val maxMessagesPerConversation: Int = DEFAULT_MAX_MESSAGES_PER_CONVERSATION,
) {
    // accessOrder = true → LinkedHashMap reorders on get() too, making it a proper LRU.
    // The synchronization is coarse-grained but the operations are cheap (map ops on
    // tens of entries) and the call sites are infrequent (one per conversation switch
    // + one debounced write per 500ms).
    private val cache = LinkedHashMap<String, List<ChatMessage>>(maxConversations, LOAD_FACTOR, true)
    private val lock = Any()

    fun read(conversationId: String): List<ChatMessage> {
        if (conversationId.isBlank()) return emptyList()
        synchronized(lock) {
            return cache[conversationId].orEmpty()
        }
    }

    fun write(
        conversationId: String,
        messages: List<ChatMessage>,
    ) {
        if (conversationId.isBlank()) return
        synchronized(lock) {
            cache[conversationId] = messages.takeLast(maxMessagesPerConversation)
            while (cache.size > maxConversations) {
                val eldest = cache.keys.iterator().next()
                cache.remove(eldest)
            }
        }
    }

    private companion object {
        private const val DEFAULT_MAX_CONVERSATIONS = 20
        private const val DEFAULT_MAX_MESSAGES_PER_CONVERSATION = 30
        private const val LOAD_FACTOR = 0.75f
    }
}
