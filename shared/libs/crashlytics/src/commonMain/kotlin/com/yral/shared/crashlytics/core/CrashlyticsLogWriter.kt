package com.yral.shared.crashlytics.core

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

class CrashlyticsLogWriter(
    private val crashlyticsManager: CrashlyticsManager,
) : LogWriter() {
    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        val formattedMessage = "[${severity.name}] $tag: $message"
        crashlyticsManager.logMessage(formattedMessage)
        throwable?.let {
            val exception = if (it is Exception) it else Exception(it)
            crashlyticsManager.recordException(exception)
        }
    }
}
