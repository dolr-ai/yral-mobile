package com.yral.shared.app.config

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.rust.service.services.RustLogForwardingListener
import com.yral.shared.rust.service.services.RustLogLevel
import com.yral.shared.rust.service.services.RustLogMessage

class AppRustLogForwardingListener(
    private val crashlyticsManager: CrashlyticsManager,
) : RustLogForwardingListener {
    override fun forwardMessage(
        logMessage: RustLogMessage,
        formattedMessage: String,
    ) {
        if (logMessage.level == RustLogLevel.ERROR) {
            crashlyticsManager.recordException(
                Exception("Rust Error: ${logMessage.message}"),
                ExceptionType.RUST,
            )
        }
    }
}
