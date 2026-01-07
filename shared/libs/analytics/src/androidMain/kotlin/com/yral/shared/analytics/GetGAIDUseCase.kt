package com.yral.shared.analytics

import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetGAIDUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val advertisingIdProvider: AdvertisingIdProvider,
) : UnitSuspendUseCase<String?>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Unit): String? = advertisingIdProvider.getAdvertisingId()
}
