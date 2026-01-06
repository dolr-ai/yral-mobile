package com.yral.shared.features.tournament.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.tournament.domain.model.CastTournamentVoteRequest
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import com.yral.shared.features.tournament.domain.model.VoteResult
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CastTournamentVoteUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val tournamentRepository: ITournamentRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
) : ResultSuspendUseCase<CastTournamentVoteRequest, VoteResult, TournamentError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: CastTournamentVoteRequest): Result<VoteResult, TournamentError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return tournamentRepository.castVote(idToken, parameter)
    }

    override fun Throwable.toError(): TournamentError =
        TournamentError(
            code = TournamentErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
