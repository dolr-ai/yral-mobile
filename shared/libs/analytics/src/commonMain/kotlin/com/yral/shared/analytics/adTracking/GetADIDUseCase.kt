package com.yral.shared.analytics.adTracking

import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

internal class GetADIDUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val advertisingIdProvider: AdvertisingIdProvider,
) : UnitSuspendUseCase<String?>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Unit): String? = advertisingIdProvider.getAdvertisingId()
}
