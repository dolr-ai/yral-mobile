package com.yral.shared.http

import co.touchlab.kermit.LogWriter
import com.yral.shared.core.logging.YralLogger
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger

class HttpLogger(
    baseLogger: YralLogger,
    additionalLogWriter: LogWriter?,
) : Logger {
    private val logger =
        (additionalLogWriter?.let { baseLogger.withAdditionalLogWriter(it) } ?: baseLogger)
            .withTag("HTTP")

    override fun log(message: String) {
        logger.d("HTTP Client $message")
    }

    val logLevel = LogLevel.BODY // todo set log level based on debug/release flag
}
