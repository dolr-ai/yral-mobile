package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseExceptionType
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CheckVideoVoteUseCase(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<CheckVideoVoteUseCase.Params, Boolean>(dispatchers.network, useCaseFailureListener) {
    override val exceptionType: UseCaseExceptionType = UseCaseExceptionType.Feed

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
