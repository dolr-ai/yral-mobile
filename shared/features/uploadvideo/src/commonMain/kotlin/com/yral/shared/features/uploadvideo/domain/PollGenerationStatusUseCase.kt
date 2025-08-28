package com.yral.shared.features.uploadvideo.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.libs.arch.domain.FlowUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper
import com.yral.shared.uniffi.generated.VideoGenRequestStatusWrapper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min

class PollGenerationStatusUseCase(
    private val repository: RateLimitRepository,
    private val config: PollingConfigProvider,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : FlowUseCase<PollGenerationStatusUseCase.Params, VideoGenRequestStatusWrapper>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = failureListener,
    ) {
    override fun execute(parameters: Params): Flow<Result<VideoGenRequestStatusWrapper, Throwable>> =
        flow {
            val completed: Boolean =
                withTimeoutOrNull(parameters.maxPollingTimeMs) {
                    var pollCount = 0
                    var currentIntervalMs = initialIntervalMs(parameters)
                    while (currentCoroutineContext().isActive) {
                        val delayMs = computeDelayMs(parameters, pollCount, currentIntervalMs)
                        delay(delayMs.coerceAtLeast(0L))
                        val status = repository.fetchVideoGenerationStatus(parameters.requestKey)
                        when (status) {
                            is Result2Wrapper.Err -> {
                                throw YralException(status.v1)
                            }

                            is Result2Wrapper.Ok -> {
                                emit(Ok(status.v1))
                                if (status.v1 is VideoGenRequestStatusWrapper.Complete ||
                                    status.v1 is VideoGenRequestStatusWrapper.Failed
                                ) {
                                    return@withTimeoutOrNull true
                                }
                            }
                        }
                        currentIntervalMs = nextIntervalMs(parameters, pollCount, currentIntervalMs)
                        pollCount++
                    }
                    true
                } ?: false
            if (!completed) {
                throw YralException("Video generation status polling timed out")
            }
        }

    private fun initialIntervalMs(parameters: Params): Long =
        if (parameters.isFastInitially) {
            config.initialIntervalMs
        } else {
            config.maxIntervalMs
        }

    private fun computeDelayMs(
        parameters: Params,
        pollCount: Int,
        currentIntervalMs: Long,
    ): Long =
        if (parameters.isFastInitially) {
            when {
                pollCount < config.earlyPolls -> config.earlyIntervalMs
                pollCount == config.earlyPolls -> config.initialIntervalMs
                else -> currentIntervalMs
            }
        } else {
            // Reverse mode: start slow with large delay and speed up gradually
            currentIntervalMs
        }

    private fun nextIntervalMs(
        parameters: Params,
        pollCount: Int,
        currentIntervalMs: Long,
    ): Long =
        if (parameters.isFastInitially) {
            // Increasing mode: exponential backoff with cap after early polls
            if (pollCount >= config.earlyPolls) {
                min(
                    a = config.maxIntervalMs,
                    b = (currentIntervalMs * config.backoffMultiplier).toLong().coerceAtLeast(0L),
                )
            } else {
                currentIntervalMs
            }
        } else {
            // Reverse mode: exponential decay towards min interval
            maxOf(
                config.minIntervalMs,
                (currentIntervalMs / config.decayMultiplier).toLong().coerceAtLeast(0L),
            )
        }

    data class Params(
        val requestKey: VideoGenRequestKeyWrapper,
        val isFastInitially: Boolean = false,
        val maxPollingTimeMs: Long = DEFAULT_MAX_POLLING_MS,
    )

    companion object {
        private const val DEFAULT_MAX_POLLING_MS = 5 * 60 * 1000L // 5 minutes
    }
}
