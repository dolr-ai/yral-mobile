package com.yral.shared.analytics.providers.onesignal

import android.app.Application
import com.onesignal.OneSignal

class AndroidOneSignal(
    private val application: Application,
) : OneSignalKMP {
    override fun initialize(appId: String) {
        OneSignal.initWithContext(context = application, appId = appId)
    }

    override fun login(externalId: String) {
        OneSignal.login(externalId)
    }

    override fun logout() {
        OneSignal.logout()
    }
}
