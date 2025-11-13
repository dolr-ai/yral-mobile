package com.yral.shared.http

import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsLogWriter
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger

class HttpLogger(
    baseLogger: YralLogger,
    crashlyticsLogWriter: CrashlyticsLogWriter,
) : Logger {
    private val logger =
        baseLogger
            .withAdditionalLogWriter(crashlyticsLogWriter)
            .withTag("HTTP")

    override fun log(message: String) {
        logger.d("HTTP Client $message")
    }

    val logLevel = LogLevel.BODY // todo set log level based on debug/release flag
}
