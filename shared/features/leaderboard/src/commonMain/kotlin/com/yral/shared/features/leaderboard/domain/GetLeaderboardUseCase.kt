package com.yral.shared.features.leaderboard.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.leaderboard.domain.models.GetLeaderboardRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardData
import com.yral.shared.features.leaderboard.domain.models.LeaderboardError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardErrorCodes
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetLeaderboardUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val leaderboardRepository: ILeaderboardRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
) : ResultSuspendUseCase<GetLeaderboardRequest, LeaderboardData, LeaderboardError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: GetLeaderboardRequest): Result<LeaderboardData, LeaderboardError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return leaderboardRepository.getLeaderboard(idToken, parameter)
    }

    override fun Throwable.toError(): LeaderboardError =
        LeaderboardError(
            code = LeaderboardErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
