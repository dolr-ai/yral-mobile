package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.useCase.SuspendUseCase

class CastVoteUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val getIdTokenUseCase: GetIdTokenUseCase,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<CastVoteRequest, CastVoteResponse>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: CastVoteRequest): CastVoteResponse {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return gameRepository
            .castVote(parameter.copy(idToken = idToken))
    }
}
