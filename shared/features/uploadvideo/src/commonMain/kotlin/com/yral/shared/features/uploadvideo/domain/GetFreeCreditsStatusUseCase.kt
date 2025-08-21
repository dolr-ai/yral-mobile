package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.RateLimitStatus

internal class GetFreeCreditsStatusUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: RateLimitRepository,
) : SuspendUseCase<GetFreeCreditsStatusUseCase.Params, RateLimitStatus>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Params): RateLimitStatus =
        repository
            .getVideoGenFreeCreditsStatus(
                canisterId = parameter.canisterId,
                isRegistered = parameter.isRegistered,
            ) ?: throw YralException("Rate limit status not found")

    data class Params(
        val canisterId: String,
        val isRegistered: Boolean,
    )
}
