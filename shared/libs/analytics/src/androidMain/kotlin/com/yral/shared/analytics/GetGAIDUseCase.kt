package com.yral.shared.analytics

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetGAIDUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val context: Context,
) : UnitSuspendUseCase<String?>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Unit): String? = AdvertisingIdClient.getAdvertisingIdInfo(context).id
}
