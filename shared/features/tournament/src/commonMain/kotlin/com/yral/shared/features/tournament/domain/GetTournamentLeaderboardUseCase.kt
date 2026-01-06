@file:Suppress("MaxLineLength")

package com.yral.shared.features.tournament.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.tournament.domain.model.GetTournamentLeaderboardRequest
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import com.yral.shared.features.tournament.domain.model.TournamentLeaderboard
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetTournamentLeaderboardUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val tournamentRepository: ITournamentRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
) : ResultSuspendUseCase<GetTournamentLeaderboardRequest, TournamentLeaderboard, TournamentError>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: GetTournamentLeaderboardRequest): Result<TournamentLeaderboard, TournamentError> {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return tournamentRepository.getTournamentLeaderboard(idToken, parameter)
    }

    override fun Throwable.toError(): TournamentError =
        TournamentError(
            code = TournamentErrorCodes.UNKNOWN,
            message = message ?: "",
            throwable = this,
        )
}
