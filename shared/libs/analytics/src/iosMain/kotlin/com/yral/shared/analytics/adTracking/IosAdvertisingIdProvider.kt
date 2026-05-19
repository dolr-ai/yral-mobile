package com.yral.shared.analytics.adTracking

import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

/**
 * iOS implementation of AdvertisingIdProvider.
 * Advertising identifier collection is intentionally disabled on iOS.
 */
class IosAdvertisingIdProvider(
    @Suppress("UnusedPrivateProperty")
    private val appDispatchers: AppDispatchers,
) : AdvertisingIdProvider {
    override suspend fun getAdvertisingId(): String? = null

    override fun getAdvertisingIdKey(): String = "advertising_id"
}
