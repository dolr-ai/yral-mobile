package com.yral.shared.preferences.stores

import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class DailyStreakLaunchStore(
    private val preferences: Preferences,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), Long.serializer())

    suspend fun getStreakCount(principal: String): Long? = getStoredCounts()[principal]

    suspend fun putStreakCount(
        principal: String,
        streakCount: Long,
    ) {
        val updatedCounts =
            getStoredCounts()
                .toMutableMap()
                .apply { put(principal, streakCount) }

        preferences.putString(
            PrefKeys.DAILY_STREAK_LAUNCH_COUNTS.name,
            json.encodeToString(serializer, updatedCounts),
        )
    }

    private suspend fun getStoredCounts(): Map<String, Long> =
        preferences
            .getString(PrefKeys.DAILY_STREAK_LAUNCH_COUNTS.name)
            ?.let { encoded ->
                runCatching {
                    json.decodeFromString(serializer, encoded)
                }.getOrDefault(emptyMap())
            } ?: emptyMap()
}
