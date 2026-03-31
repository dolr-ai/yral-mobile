package com.yral.shared.app.config

import com.yral.shared.core.AppConfigurations
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.http.HTTPEventListener
import com.yral.shared.http.exception.DNSLookupException

class AppHTTPEventListener(
    private val crashlyticsManager: CrashlyticsManager,
) : HTTPEventListener {
    override fun logException(e: Exception) {
        if (e is DNSLookupException) {
            val type =
                if (AppConfigurations.isAuthHost(e.hostname)) {
                    ExceptionType.AUTH
                } else {
                    ExceptionType.UNKNOWN
                }
            crashlyticsManager.logMessage(
                "dns_lookup_failure host=${e.hostname} source=${e.lookupSource} error_type=${type.name.lowercase()}",
            )
            crashlyticsManager.recordException(e, type)
            return
        }
        crashlyticsManager.recordException(e)
    }
}
