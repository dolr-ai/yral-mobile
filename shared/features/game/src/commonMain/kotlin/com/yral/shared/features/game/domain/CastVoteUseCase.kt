package com.yral.shared.features.game.domain

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.libs.useCase.SuspendUseCase

class CastVoteUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<CastVoteRequest, CastVoteResponse>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: CastVoteRequest): CastVoteResponse =
        gameRepository
            .castVote(parameter)
}
