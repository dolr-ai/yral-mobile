package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.domain.models.Collage

/**
 * In-memory LRU cache for daily collage payloads, keyed by
 * (botId, date, isSubscribed) so a subscription flip is a natural cache
 * miss — the refetch with the new state returns the other URL set
 * (clear vs pre-blurred) and every collage bubble self-heals.
 *
 * Same multiplatform constraints as [ConversationContentCache]: plain
 * LinkedHashMap with manual recency bumps (the JVM accessOrder overload
 * doesn't exist on iOS Native) and no `synchronized` (JVM-only). All call
 * sites run on `viewModelScope` (Main dispatcher), so access is
 * thread-confined in practice.
 *
 * In-memory only, by design: URLs must never outlive the subscription
 * state they were fetched under.
 */
class CollageCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    private val cache = LinkedHashMap<String, Collage>()

    fun read(
        botId: String,
        date: String,
        isSubscribed: Boolean,
    ): Collage? {
        val key = key(botId, date, isSubscribed)
        val value = cache[key] ?: return null
        // Bump recency by removing + re-inserting at the tail.
        cache.remove(key)
        cache[key] = value
        return value
    }

    fun write(
        collage: Collage,
        isSubscribed: Boolean,
    ) {
        val key = key(collage.botId, collage.date, isSubscribed)
        cache.remove(key)
        cache[key] = collage
        while (cache.size > maxEntries) {
            val eldest = cache.keys.iterator().next()
            cache.remove(eldest)
        }
    }

    private fun key(
        botId: String,
        date: String,
        isSubscribed: Boolean,
    ): String = "$botId|$date|$isSubscribed"

    private companion object {
        private const val DEFAULT_MAX_ENTRIES = 30
    }
}
