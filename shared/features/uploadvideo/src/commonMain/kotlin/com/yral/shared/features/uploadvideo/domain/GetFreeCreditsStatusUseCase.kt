package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.RateLimitStatusWrapper

internal class GetFreeCreditsStatusUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: RateLimitRepository,
) : SuspendUseCase<GetFreeCreditsStatusUseCase.Params, RateLimitStatusWrapper>(
        appDispatchers.network,
        failureListener,
    ) {
    override suspend fun execute(parameter: Params): RateLimitStatusWrapper =
        repository
            .getVideoGenFreeCreditsStatus(
                userPrincipal = parameter.userPrincipal,
                isRegistered = parameter.isRegistered,
            ) ?: throw YralException("Rate limit status not found")

    data class Params(
        val userPrincipal: String,
        val isRegistered: Boolean,
    )
}
