package com.yral.shared.features.auth.data

import com.yral.shared.core.AppConfigurations.OAUTH_BASE_URL
import com.yral.shared.core.AppConfigurations.OAUTH_FALLBACK_BASE_URL
import com.yral.shared.crashlytics.core.CrashlyticsManager

class SessionAuthHostResolver(
    private val crashlyticsManager: CrashlyticsManager,
) {
    private var selectedHost: String = OAUTH_BASE_URL

    fun currentHost(): String = selectedHost

    fun isFallbackActive(): Boolean = selectedHost == OAUTH_FALLBACK_BASE_URL

    fun activateFallback(failedHost: String): String {
        if (!isFallbackActive()) {
            selectedHost = OAUTH_FALLBACK_BASE_URL
            crashlyticsManager.logMessage(
                "auth_host_fallback_activated failed_host=$failedHost selected_host=$selectedHost",
            )
        }
        return selectedHost
    }
}
