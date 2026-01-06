@file:Suppress("MaxLineLength")

package com.yral.shared.features.tournament.domain

import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.GetTournamentStatusRequest
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import com.yral.shared.features.tournament.domain.model.TournamentStatusData
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetTournamentStatusUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val tournamentRepository: ITournamentRepository,
) : ResultSuspendUseCase<GetTournamentStatusRequest, TournamentStatusData, TournamentError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: GetTournamentStatusRequest): Result<TournamentStatusData, TournamentError> =
        tournamentRepository.getTournamentStatus(parameter)

    override fun Throwable.toError(): TournamentError =
        TournamentError(
            code = TournamentErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
