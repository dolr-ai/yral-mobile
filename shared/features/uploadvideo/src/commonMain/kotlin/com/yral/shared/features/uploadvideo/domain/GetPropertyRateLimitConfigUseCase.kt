package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.RateLimitRepository
import com.yral.shared.rust.service.domain.models.PropertyRateLimitConfig

internal class GetPropertyRateLimitConfigUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: RateLimitRepository,
) : SuspendUseCase<GetPropertyRateLimitConfigUseCase.Params, PropertyRateLimitConfig?>(
        appDispatchers.network,
        failureListener,
    ) {
    override val exceptionType: String = ExceptionType.AI_VIDEO.name

    override suspend fun execute(parameter: Params): PropertyRateLimitConfig? =
        repository.getPropertyRateLimitConfig(
            userPrincipal = parameter.userPrincipal,
            property = VIDEO_GEN_PROPERTY,
        )

    data class Params(
        val userPrincipal: String,
    )

    private companion object {
        const val VIDEO_GEN_PROPERTY = "VIDEOGEN"
    }
}
