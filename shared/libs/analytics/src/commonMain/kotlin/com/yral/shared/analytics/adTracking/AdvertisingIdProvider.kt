package com.yral.shared.analytics.adTracking

interface AdvertisingIdProvider {
    suspend fun getAdvertisingId(): String?
    fun getAdvertisingIdKey(): String
}
