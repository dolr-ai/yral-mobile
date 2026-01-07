package com.yral.shared.analytics

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient

class AndroidAdvertisingIdProvider(
    private val applicationContext: Context,
) : AdvertisingIdProvider {
    override suspend fun getAdvertisingId(): String? =
        AdvertisingIdClient
            .getAdvertisingIdInfo(applicationContext)
            .id
}
