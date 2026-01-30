package com.yral.shared.features.tournament.domain

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.yral.shared.features.tournament.domain.model.VideoEmojisResult
import com.yral.shared.libs.arch.domain.ResultSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

/**
 * Request parameters for fetching video-specific emojis.
 */
data class GetVideoEmojisRequest(
    val tournamentId: String,
    val videoId: String,
)

/**
 * Use case to fetch video-specific emojis for a tournament video.
 * Used for prefetching emoji data before user sees the video.
 */
class GetVideoEmojisUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val tournamentRepository: ITournamentRepository,
) : ResultSuspendUseCase<GetVideoEmojisRequest, VideoEmojisResult, Throwable>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun executeWith(parameter: GetVideoEmojisRequest): Result<VideoEmojisResult, Throwable> =
        tournamentRepository
            .getVideoEmojis(parameter.tournamentId, parameter.videoId)
            .mapError { it }

    override fun Throwable.toError(): Throwable = this
}
