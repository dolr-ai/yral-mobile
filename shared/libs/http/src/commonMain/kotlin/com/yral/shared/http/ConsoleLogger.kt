package com.yral.shared.http

import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger

class ConsoleLogger : Logger {
    override fun log(message: String) {
        println("xxxx HTTP Client $message")
    }
    val logLevel = LogLevel.BODY
}
