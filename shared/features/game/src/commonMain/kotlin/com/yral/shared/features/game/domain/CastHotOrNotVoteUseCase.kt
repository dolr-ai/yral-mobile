package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.game.domain.models.CastHotOrNotVoteRequest
import com.yral.shared.features.game.domain.models.CastHotOrNotVoteResponse
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CastHotOrNotVoteUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val getIdTokenUseCase: GetIdTokenUseCase,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<CastHotOrNotVoteRequest, CastHotOrNotVoteResponse>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: CastHotOrNotVoteRequest): CastHotOrNotVoteResponse {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return gameRepository
            .castHotOrNotVote(parameter.copy(idToken = idToken))
    }
}
