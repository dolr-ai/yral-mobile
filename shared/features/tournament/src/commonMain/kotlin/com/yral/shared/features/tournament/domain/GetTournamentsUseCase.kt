package com.yral.shared.features.tournament.domain

import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.GetTournamentsRequest
import com.yral.shared.features.tournament.domain.model.TournamentData
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetTournamentsUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val tournamentRepository: ITournamentRepository,
) : ResultSuspendUseCase<GetTournamentsRequest, List<TournamentData>, TournamentError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: GetTournamentsRequest): Result<List<TournamentData>, TournamentError> =
        tournamentRepository.getTournaments(parameter)

    override fun Throwable.toError(): TournamentError =
        TournamentError(
            code = TournamentErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
