package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CheckVideoVoteUseCase(
    dispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<CheckVideoVoteUseCase.Params, Boolean>(dispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.FEED.name

    // Firebase-backed vote lookup was removed in this branch, so this remains a no-op.
    override suspend fun execute(parameter: Params): Boolean = false

    data class Params(
        val videoId: String,
        val principalId: String,
    )
}
