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
    // Multiplatform-safe LRU: a plain LinkedHashMap maintains insertion order
    // on every Kotlin target, and the `read` path removes + re-inserts to
    // bump the touched key to the most-recently-used end. The JVM
    // `LinkedHashMap(initialCapacity, loadFactor, accessOrder)` overload is
    // JVM-only; on iOS Native it doesn't exist (different platform-specific
    // implementation), so we implement access-order manually.
    //
    // Thread confinement: both call sites (the `read` from
    // `ConversationViewModel.setConversationId` and the `write` from the
    // VM's debounced flow-collect) run on the same `viewModelScope`
    // coroutine context (Main dispatcher), so no cross-thread access
    // happens in practice. We deliberately do NOT call `synchronized` here
    // because it's JVM-only and breaks iOS native compile; adding a
    // multiplatform lock (atomicfu, Mutex) would be overkill for a
    // best-effort UX cache where a torn read at worst returns an empty
    // list (the call site already handles that as "cache miss").
    private val cache = LinkedHashMap<String, List<ChatMessage>>()

    fun read(conversationId: String): List<ChatMessage> {
        if (conversationId.isBlank()) return emptyList()
        val value = cache[conversationId].orEmpty()
        if (value.isNotEmpty()) {
            // Bump recency by removing + re-inserting at the tail.
            cache.remove(conversationId)
            cache[conversationId] = value
        }
        return value
    }

    fun write(
        conversationId: String,
        messages: List<ChatMessage>,
    ) {
        if (conversationId.isBlank()) return
        // Re-insert at the tail regardless of whether the key already exists,
        // so a write also counts as a recency bump.
        cache.remove(conversationId)
        cache[conversationId] = messages.takeLast(maxMessagesPerConversation)
        while (cache.size > maxConversations) {
            val eldest = cache.keys.iterator().next()
            cache.remove(eldest)
        }
    }

    private companion object {
        private const val DEFAULT_MAX_CONVERSATIONS = 20
        private const val DEFAULT_MAX_MESSAGES_PER_CONVERSATION = 30
    }
}
