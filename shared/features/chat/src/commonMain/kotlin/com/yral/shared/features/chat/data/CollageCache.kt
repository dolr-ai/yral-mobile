package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.domain.models.Collage

/**
 * In-memory LRU cache for daily collage payloads, keyed by
 * (botId, date, isSubscribed) so a subscription flip is a natural cache
 * miss — the refetch with the new state returns the other URL set
 * (clear vs pre-blurred) and every collage bubble self-heals.
 *
 * Entries carry their fetch time: the server's signed image URLs expire
 * after 15 minutes (URL rotation, by design), so reads reject anything
 * older than [COLLAGE_URL_TTL_MS] and the caller refetches fresh URLs.
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
    data class Entry(
        val collage: Collage,
        val storedAtMs: Long,
    )

    private val cache = LinkedHashMap<String, Entry>()

    fun read(
        botId: String,
        date: String,
        isSubscribed: Boolean,
        nowMs: Long,
    ): Entry? {
        val key = key(botId, date, isSubscribed)
        val value = cache[key]
        return when {
            value == null -> null
            nowMs - value.storedAtMs >= COLLAGE_URL_TTL_MS -> {
                cache.remove(key)
                null
            }
            else -> {
                // Bump recency by removing + re-inserting at the tail.
                cache.remove(key)
                cache[key] = value
                value
            }
        }
    }

    fun write(
        collage: Collage,
        isSubscribed: Boolean,
        nowMs: Long,
    ) {
        val key = key(collage.botId, collage.date, isSubscribed)
        cache.remove(key)
        cache[key] = Entry(collage = collage, storedAtMs = nowMs)
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

    companion object {
        private const val DEFAULT_MAX_ENTRIES = 30

        /**
         * Signed collage URLs expire server-side after 15 min; refetch
         * with a 3 min safety margin so a bubble never renders a URL
         * that dies mid-load.
         */
        const val COLLAGE_URL_TTL_MS: Long = 12 * 60 * 1000L
    }
}
