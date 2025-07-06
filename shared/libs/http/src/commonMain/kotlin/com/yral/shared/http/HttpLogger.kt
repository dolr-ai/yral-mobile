package com.yral.shared.http

import com.yral.shared.core.logging.YralLogger
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger

class HttpLogger(
    private val logger: YralLogger,
) : Logger {
    override fun log(message: String) {
        logger.d("HTTP Client $message")
    }
    val logLevel = LogLevel.BODY // todo set log level based on debug/release flag
}
