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
        if (throwable != null) {
            Sentry.captureException(throwable) { scope ->
                scope.level = severity.toSentryLevel()
                scope.setExtra(LOG_MESSAGE_EXTRA_KEY, formattedMessage)
                scope.setTag(LOG_TAG_KEY, tag)
                scope.setTag(LOG_SEVERITY_KEY, severity.name)
            }
            return
        }

        Sentry.addBreadcrumb(Breadcrumb.info(formattedMessage))
        Sentry.captureMessage(formattedMessage) { scope ->
            scope.level = severity.toSentryLevel()
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

    private companion object {
        const val LOG_MESSAGE_EXTRA_KEY = "log_message"
        const val LOG_TAG_KEY = "log_tag"
        const val LOG_SEVERITY_KEY = "log_severity"
    }
}
