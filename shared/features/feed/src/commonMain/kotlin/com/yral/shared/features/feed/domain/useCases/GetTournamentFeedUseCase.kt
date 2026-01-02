package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.models.PostResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetTournamentFeedUseCase(
    private val feedRepository: IFeedRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GetTournamentFeedUseCase.Params, PostResponse>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.FEED.name

    override suspend fun execute(parameter: Params): PostResponse =
        feedRepository.getTournamentFeeds(
            tournamentId = parameter.tournamentId,
            withMetadata = parameter.withMetadata,
        )

    data class Params(
        val tournamentId: String,
        val withMetadata: Boolean = true,
    )
}
