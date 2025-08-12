package com.yral.shared.features.uploadvideo.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.libs.arch.domain.FlowUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.uniffi.generated.VideoGenRequestKey
import com.yral.shared.uniffi.generated.VideoGenRequestStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PollGenerationStatusUseCase(
    private val repository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : FlowUseCase<PollGenerationStatusUseCase.Params, VideoGenRequestStatus>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = failureListener,
    ) {
    override fun execute(parameters: Params): Flow<Result<VideoGenRequestStatus, Throwable>> =
        flow {
            while (true) {
                val status = repository.fetchVideoGenerationStatus(parameters.requestKey)
                emit(Ok(status))
                if (status is VideoGenRequestStatus.Complete || status is VideoGenRequestStatus.Failed) {
                    break
                }
                delay(parameters.intervalMs)
            }
        }

    data class Params(
        val requestKey: VideoGenRequestKey,
        val intervalMs: Long = DEFAULT_INTERVAL_MS,
    )

    companion object {
        private const val DEFAULT_INTERVAL_MS = 15000L // 15 seconds
    }
}
