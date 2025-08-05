package com.yral.shared.features.game.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.domain.models.GetLeaderboardRequest
import com.yral.shared.features.game.domain.models.LeaderboardData
import com.yral.shared.features.game.domain.models.LeaderboardError
import com.yral.shared.features.game.domain.models.LeaderboardErrorCodes
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.useCase.ResultSuspendUseCase

class GetLeaderboardUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val gameRepository: IGameRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
) : ResultSuspendUseCase<GetLeaderboardRequest, LeaderboardData, LeaderboardError>(
        appDispatchers.io,
        crashlyticsManager,
    ) {
    override suspend fun executeWith(parameter: GetLeaderboardRequest): Result<LeaderboardData, LeaderboardError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return gameRepository.getLeaderboard(idToken, parameter)
    }

    override fun Throwable.toError(): LeaderboardError =
        LeaderboardError(
            code = LeaderboardErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
