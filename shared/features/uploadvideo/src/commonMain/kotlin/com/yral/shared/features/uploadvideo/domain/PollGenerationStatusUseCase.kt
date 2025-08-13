package com.yral.shared.features.uploadvideo.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.libs.arch.domain.FlowUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.PollResult2
import com.yral.shared.uniffi.generated.VideoGenRequestKey
import com.yral.shared.uniffi.generated.VideoGenRequestStatus
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

class PollGenerationStatusUseCase(
    private val repository: RateLimitRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : FlowUseCase<PollGenerationStatusUseCase.Params, VideoGenRequestStatus>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = failureListener,
    ) {
    override fun execute(parameters: Params): Flow<Result<VideoGenRequestStatus, Throwable>> =
        flow {
            withTimeoutOrNull(parameters.maxPollingTimeMs) {
                while (currentCoroutineContext().isActive) {
                    delay(parameters.intervalMs)
                    val status = repository.fetchVideoGenerationStatus(parameters.requestKey)
                    when (status) {
                        is PollResult2.Err -> {
                            throw YralException(status.v1)
                        }

                        is PollResult2.Ok -> {
                            emit(Ok(status.v1))
                            if (status.v1 is VideoGenRequestStatus.Complete ||
                                status.v1 is VideoGenRequestStatus.Failed
                            ) {
                                break
                            }
                        }
                    }
                }
            }
        }

    data class Params(
        val requestKey: VideoGenRequestKey,
        val intervalMs: Long = DEFAULT_INTERVAL_MS,
        val maxPollingTimeMs: Long = DEFAULT_MAX_POLLING_MS,
    )

    companion object {
        private const val DEFAULT_INTERVAL_MS = 15 * 1000L // 15 seconds
        private const val DEFAULT_MAX_POLLING_MS = 5 * 60 * 1000L // 5 Mins
    }
}
