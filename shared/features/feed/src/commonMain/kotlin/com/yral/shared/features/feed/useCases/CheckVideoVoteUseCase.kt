package com.yral.shared.features.feed.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase

class CheckVideoVoteUseCase(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<CheckVideoVoteUseCase.Params, Boolean>(dispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Params): Boolean =
        try {
            repository
                .documentExists(
                    path = "videos/${parameter.videoId}/votes/${parameter.principalId}",
                ).getOrThrow()
        } catch (_: Exception) {
            false
        }

    data class Params(
        val videoId: String,
        val principalId: String,
    )
}
