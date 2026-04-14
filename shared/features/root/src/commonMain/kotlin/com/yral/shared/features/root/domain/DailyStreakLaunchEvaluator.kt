package com.yral.shared.features.root.domain

import com.yral.shared.preferences.stores.DailyStreakLaunchStore

class DailyStreakLaunchEvaluator(
    private val dailyStreakLaunchStore: DailyStreakLaunchStore,
) {
    suspend fun evaluate(
        principal: String,
        remoteStreakCount: Long,
    ): DailyStreakLaunchResult {
        val localStreakCount = dailyStreakLaunchStore.getStreakCount(principal)

        return when {
            localStreakCount == null -> {
                dailyStreakLaunchStore.putStreakCount(principal, remoteStreakCount)
                DailyStreakLaunchResult.NoChange
            }

            remoteStreakCount > localStreakCount -> {
                dailyStreakLaunchStore.putStreakCount(principal, remoteStreakCount)
                DailyStreakLaunchResult.ShowCelebration(remoteStreakCount)
            }

            remoteStreakCount < localStreakCount -> {
                dailyStreakLaunchStore.putStreakCount(principal, remoteStreakCount)
                DailyStreakLaunchResult.NoChange
            }

            else -> {
                DailyStreakLaunchResult.NoChange
            }
        }
    }
}

sealed interface DailyStreakLaunchResult {
    data object NoChange : DailyStreakLaunchResult

    data class ShowCelebration(
        val streakCount: Long,
    ) : DailyStreakLaunchResult
}
