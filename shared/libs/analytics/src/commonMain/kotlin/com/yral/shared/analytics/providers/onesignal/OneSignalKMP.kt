package com.yral.shared.analytics.providers.onesignal

interface OneSignalKMP {
    fun initialize(appId: String)
    fun login(externalId: String)
    fun logout()
}
