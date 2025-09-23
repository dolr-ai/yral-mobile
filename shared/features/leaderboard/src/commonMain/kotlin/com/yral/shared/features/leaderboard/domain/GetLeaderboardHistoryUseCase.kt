package com.yral.shared.features.leaderboard.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.leaderboard.domain.models.LeaderboardErrorCodes
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistory
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryRequest
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetLeaderboardHistoryUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val leaderboardRepository: ILeaderboardRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
) : ResultSuspendUseCase<LeaderboardHistoryRequest, LeaderboardHistory, LeaderboardHistoryError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    @Suppress("MaxLineLength")
    override suspend fun executeWith(parameter: LeaderboardHistoryRequest): Result<LeaderboardHistory, LeaderboardHistoryError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return leaderboardRepository.getLeaderboardHistory(idToken, parameter)
    }

    override fun Throwable.toError(): LeaderboardHistoryError =
        LeaderboardHistoryError(
            code = LeaderboardErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
