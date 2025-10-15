package com.yral.shared.app.config

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.http.HTTPEventListener

class AppHTTPEventListener(
    private val crashlyticsManager: CrashlyticsManager,
) : HTTPEventListener {
    override fun logException(e: Exception) {
        crashlyticsManager.recordException(e)
    }
}
