package com.yral.shared.features.leaderboard.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRank
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRankRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardErrorCodes
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetLeaderboardRankForTodayUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val leaderboardRepository: ILeaderboardRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
) : ResultSuspendUseCase<LeaderboardDailyRankRequest, LeaderboardDailyRank, LeaderboardError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    @Suppress("MaxLineLength")
    override suspend fun executeWith(parameter: LeaderboardDailyRankRequest): Result<LeaderboardDailyRank, LeaderboardError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return leaderboardRepository.getLeaderboardRankForToday(idToken, parameter)
    }

    override fun Throwable.toError(): LeaderboardError =
        LeaderboardError(
            code = LeaderboardErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
