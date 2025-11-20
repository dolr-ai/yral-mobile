package com.yral.shared.app.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

internal class SentryLogWriter : LogWriter() {
    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        val formattedMessage = "[${severity.name}] $tag: $message"
        Sentry.addBreadcrumb(Breadcrumb.info(formattedMessage))
        Sentry.captureMessage(formattedMessage) { scope ->
            scope.level = severity.toSentryLevel()
        }
        throwable?.let {
            Sentry.captureException(it)
        }
    }

    private fun Severity.toSentryLevel(): SentryLevel =
        when (this) {
            Severity.Verbose -> SentryLevel.DEBUG
            Severity.Debug -> SentryLevel.DEBUG
            Severity.Info -> SentryLevel.INFO
            Severity.Warn -> SentryLevel.WARNING
            Severity.Error -> SentryLevel.ERROR
            Severity.Assert -> SentryLevel.FATAL
        }
}
