package com.yral.shared.app.config

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.rust.service.services.RustLogForwardingListener
import com.yral.shared.rust.service.services.RustLogLevel
import com.yral.shared.rust.service.services.RustLogMessage

class AppRustLogForwardingListener(
    private val crashlyticsManager: CrashlyticsManager,
    private val logWriter: LogWriter,
) : RustLogForwardingListener {
    override fun forwardMessage(
        logMessage: RustLogMessage,
        formattedMessage: String,
    ) {
        val severity = logMessage.level.toSeverity()
        logWriter.log(
            severity = severity,
            message = formattedMessage,
            tag = "Rust",
            throwable = null,
        )

        if (logMessage.level == RustLogLevel.ERROR) {
            crashlyticsManager.recordException(
                Exception("Rust Error: ${logMessage.message}"),
                ExceptionType.RUST,
            )
        }
    }

    private fun RustLogLevel.toSeverity(): Severity =
        when (this) {
            RustLogLevel.TRACE -> Severity.Verbose
            RustLogLevel.DEBUG -> Severity.Debug
            RustLogLevel.INFO -> Severity.Info
            RustLogLevel.WARN -> Severity.Warn
            RustLogLevel.ERROR -> Severity.Error
        }
}
