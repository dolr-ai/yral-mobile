package com.yral.shared.features.uploadvideo.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import com.yral.shared.libs.arch.domain.FlowUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.PollResult2
import com.yral.shared.uniffi.generated.VideoGenRequestKey
import com.yral.shared.uniffi.generated.VideoGenRequestStatus
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlin.math.min

internal class PollAndUploadAiVideoUseCase(
    private val rateLimitRepository: RateLimitRepository,
    private val uploadRepository: UploadRepository,
    private val config: PollingConfigProvider,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : FlowUseCase<PollAndUploadAiVideoUseCase.Params, PollAndUploadAiVideoUseCase.PollAndUploadResult>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = failureListener,
    ) {
    override fun execute(parameters: Params): Flow<Result<PollAndUploadResult, Throwable>> =
        flow {
            var pollCount = 0
            try {
                withTimeout(parameters.maxPollingTimeMs) {
                    var currentIntervalMs = initialIntervalMs(parameters)
                    while (currentCoroutineContext().isActive) {
                        val delayMs = computeDelayMs(parameters, pollCount, currentIntervalMs)
                        delay(delayMs.coerceAtLeast(0L))
                        val status =
                            rateLimitRepository.fetchVideoGenerationStatus(parameters.requestKey)
                        when (status) {
                            is PollResult2.Err -> {
                                emit(Ok(PollAndUploadResult.Failed(status.v1)))
                                return@withTimeout
                            }

                            is PollResult2.Ok -> {
                                when (val videoStatus = status.v1) {
                                    is VideoGenRequestStatus.Complete -> {
                                        // Upload the video when generation is complete
                                        uploadRepository.uploadAiVideoFromUrl(
                                            request =
                                                UploadAiVideoFromUrlRequest(
                                                    videoUrl = videoStatus.v1,
                                                    hashtags = parameters.hashtags,
                                                    description = parameters.description,
                                                    isNsfw = parameters.isNsfw,
                                                    enableHotOrNot = parameters.enableHotOrNot,
                                                ),
                                        )
                                        emit(Ok(PollAndUploadResult.Success(videoStatus.v1)))
                                        return@withTimeout
                                    }

                                    is VideoGenRequestStatus.Failed -> {
                                        emit(Ok(PollAndUploadResult.Failed(videoStatus.v1)))
                                        return@withTimeout
                                    }

                                    VideoGenRequestStatus.Pending,
                                    VideoGenRequestStatus.Processing,
                                    -> {
                                        emit(Ok(PollAndUploadResult.InProgress(pollCount)))
                                    }
                                }
                            }
                        }
                        currentIntervalMs = nextIntervalMs(parameters, pollCount, currentIntervalMs)
                        pollCount++
                    }
                }
            } catch (e: TimeoutCancellationException) {
                throw VideoGenerationTimeoutException.fromTimeoutCancellation(
                    timeoutException = e,
                    requestKey = parameters.requestKey.toString(),
                    timeout = parameters.maxPollingTimeMs,
                    pollCount = pollCount,
                )
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
        val requestKey: VideoGenRequestKey,
        val isFastInitially: Boolean = false,
        val maxPollingTimeMs: Long = DEFAULT_MAX_POLLING_MS,
        val hashtags: List<String> = emptyList(),
        val description: String = "",
        val isNsfw: Boolean = false,
        val enableHotOrNot: Boolean = false,
    )

    sealed class PollAndUploadResult {
        data class InProgress(
            val pollCount: Int,
        ) : PollAndUploadResult()
        data class Success(
            val videoUrl: String,
        ) : PollAndUploadResult()
        data class Failed(
            val errorMessage: String,
        ) : PollAndUploadResult()
    }

    companion object {
        private const val DEFAULT_MAX_POLLING_MS = 5 * 60 * 1000L // 5 minutes
    }
}

class VideoGenerationTimeoutException(
    override val message: String,
    override val cause: TimeoutCancellationException,
    val requestKey: String? = null,
    val timeout: Long = 0,
    val pollCount: Int = 0,
) : Exception() {
    companion object {
        fun fromTimeoutCancellation(
            timeoutException: TimeoutCancellationException,
            requestKey: String? = null,
            timeout: Long = 0,
            pollCount: Int = 0,
        ): VideoGenerationTimeoutException =
            VideoGenerationTimeoutException(
                message = "Video generation timed out after $timeout Ms",
                cause = timeoutException,
                requestKey = requestKey,
                timeout = timeout,
                pollCount = pollCount,
            )
    }
}
