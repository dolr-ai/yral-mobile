package com.yral.shared.analytics

interface AdvertisingIdProvider {
    suspend fun getAdvertisingId(): String?
}
