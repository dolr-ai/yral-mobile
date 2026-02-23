package com.yral.shared.preferences.stores

import com.yral.shared.core.session.AccountDirectory
import com.yral.shared.core.session.AccountDirectoryCache
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.serialization.json.Json

class AccountDirectoryStore(
    private val preferences: Preferences,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(): AccountDirectory? =
        preferences
            .getString(PrefKeys.ACCOUNT_DIRECTORY_CACHE.name)
            ?.let { encoded ->
                runCatching {
                    json.decodeFromString(AccountDirectoryCache.serializer(), encoded)
                }.getOrNull()
            }?.toAccountDirectory()

    suspend fun put(directory: AccountDirectory) {
        val encoded =
            json.encodeToString(
                AccountDirectoryCache.serializer(),
                AccountDirectoryCache.from(directory),
            )
        preferences.putString(PrefKeys.ACCOUNT_DIRECTORY_CACHE.name, encoded)
    }

    suspend fun remove() {
        preferences.remove(PrefKeys.ACCOUNT_DIRECTORY_CACHE.name)
    }
}
