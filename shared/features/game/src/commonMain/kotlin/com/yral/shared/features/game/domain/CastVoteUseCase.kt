package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CastVoteUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val getIdTokenUseCase: GetIdTokenUseCase,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<CastVoteRequest, CastVoteResponse>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: CastVoteRequest): CastVoteResponse {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return gameRepository
            .castVote(parameter.copy(idToken = idToken))
    }
}
