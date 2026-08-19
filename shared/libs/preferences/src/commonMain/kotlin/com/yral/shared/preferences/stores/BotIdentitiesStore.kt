package com.yral.shared.preferences.stores

import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class BotIdentitiesStore(
    private val preferences: Preferences,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(BotIdentityEntry.serializer())

    suspend fun get(): List<BotIdentityEntry> =
        preferences
            .getString(PrefKeys.BOT_IDENTITIES.name)
            ?.let { encoded ->
                runCatching {
                    json.decodeFromString(listSerializer, encoded)
                }.getOrNull()
            } ?: emptyList()

    suspend fun put(entries: List<BotIdentityEntry>) {
        val encoded = json.encodeToString(listSerializer, entries)
        preferences.putString(PrefKeys.BOT_IDENTITIES.name, encoded)
    }

    suspend fun remove() {
        preferences.remove(PrefKeys.BOT_IDENTITIES.name)
    }

    /**
     * Merges bot account IDs from the JWT's `ext_ai_account_ids` claim
     * with existing entries and persists the result.
     */
    @Suppress("ReturnCount")
    suspend fun mergeFromTokenBotAccountIds(botAccountIds: List<String>): MergeFromTokenResult? {
        if (botAccountIds.isEmpty()) return null
        val entries = botAccountIds.map { BotIdentityEntry(principal = it) }.filter { it.principal.isNotBlank() }
        if (entries.isEmpty()) return null
        val existing = get()
        val merged =
            (existing + entries)
                .groupBy { it.principal }
                .map { (_, list) ->
                    val latest = list.last()
                    val username =
                        list
                            .asReversed()
                            .firstOrNull { !it.username.isNullOrBlank() }
                            ?.username
                    latest.copy(username = username)
                }
        put(merged)
        return MergeFromTokenResult(
            existingCount = existing.size,
            addedCount = entries.size,
            mergedCount = merged.size,
        )
    }

    data class MergeFromTokenResult(
        val existingCount: Int,
        val addedCount: Int,
        val mergedCount: Int,
    )
}

@Serializable
data class BotIdentityEntry(
    val principal: String,
    val username: String? = null,
)
