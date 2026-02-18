package com.yral.shared.preferences.stores

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
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
     * Parses raw identity payloads from an OAuth token (e.g. [botDelegatedIdentities]),
     * resolves principal via [principalFromIdentityBytes], merges with existing entries and persists.
     * JSON decode/encode of [KotlinDelegatedIdentityWire] is done here;
     * principal resolution stays in the caller (e.g. Rust FFI).
     */
    @Suppress("ReturnCount")
    suspend fun mergeFromOAuthTokenRawIdentities(
        rawPayloads: List<ByteArray>,
        principalFromIdentityBytes: (ByteArray) -> String,
        onEntryParseFailure: ((raw: ByteArray, error: Throwable) -> Unit)? = null,
    ): MergeFromTokenResult? {
        if (rawPayloads.isEmpty()) return null
        val entries =
            rawPayloads
                .mapNotNull { raw ->
                    runCatching {
                        val decodedString = raw.decodeToString()
                        val wire =
                            runCatching { json.decodeFromString<KotlinDelegatedIdentityWire>(decodedString) }
                                .getOrElse {
                                    val base64Decoded = decodedString.decodeBase64Bytes()
                                    json.decodeFromString<KotlinDelegatedIdentityWire>(
                                        base64Decoded.decodeToString(),
                                    )
                                }
                        val encoded = json.encodeToString(wire).encodeToByteArray()
                        val principal = principalFromIdentityBytes(encoded)
                        BotIdentityEntry(principal = principal, identity = encoded.encodeBase64())
                    }.onFailure { error -> onEntryParseFailure?.invoke(raw, error) }.getOrNull()
                }.filter { it.principal.isNotBlank() }
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
    val identity: String,
    val username: String? = null,
)
