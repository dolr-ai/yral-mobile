package com.yral.shared.rust.service.services

import co.touchlab.kermit.Logger
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.uniffi.generated.LogLevel
import com.yral.shared.uniffi.generated.LogMessage
import com.yral.shared.uniffi.generated.LoggerException
import com.yral.shared.uniffi.generated.getLogMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Service for forwarding Rust logs to external logging services like Crashlytics or Sentry.
 *
 * This service polls the Rust logger for new log messages and forwards them to the configured
 * external logging service. This approach is used because UniFFI doesn't support callback
 * interfaces in the current version.
 */
@Suppress("TooGenericExceptionCaught")
internal class LogForwardingService(
    private val appDispatchers: AppDispatchers,
    private val forwarder: RustLogForwardingListener,
) {
    private val logger = Logger.withTag("LogForwardingService")
    private var forwardingJob: Job? = null
    private var isRunning = false

    /**
     * Start forwarding logs to external services.
     *
     * @param intervalMs The interval in milliseconds to check for new log messages
     * @param batchSize The maximum number of log messages to process in each batch
     */
    fun startForwarding(
        intervalMs: Long = 1000L,
        recoveryDelayMs: Long = 5000L,
        batchSize: Int = 50,
    ) {
        if (isRunning) {
            logger.w { "Log forwarding is already running" }
            return
        }

        isRunning = true
        forwardingJob =
            CoroutineScope(SupervisorJob() + appDispatchers.cpu).launch {
                logger.i { "Starting log forwarding service" }
                while (isActive && isRunning) {
                    try {
                        val logMessages = getLogMessages(batchSize.toUInt())
                        if (logMessages.isNotEmpty()) {
                            logger.d { "Processing ${logMessages.size} log messages" }
                            logMessages.forEach { logMessage -> forwardLogMessage(logMessage) }
                        }
                        delay(intervalMs)
                    } catch (e: Exception) {
                        logger.e(e) { "Error in log forwarding loop" }
                        delay(recoveryDelayMs) // Wait longer on error
                    }
                }
                logger.i { "Log forwarding service stopped" }
            }
    }

    /**
     * Stop the log forwarding service.
     */
    fun stopForwarding() {
        if (!isRunning) {
            logger.w { "Log forwarding is not running" }
            return
        }
        isRunning = false
        forwardingJob?.cancel()
        forwardingJob = null
        logger.i { "Stopping log forwarding service" }
    }

    /**
     * Forward a single log message to external services.
     *
     * @param logMessage The log message to forward
     */
    private fun forwardLogMessage(logMessage: LogMessage) {
        try {
            val formattedMessage = "[${logMessage.tag}] ${logMessage.level} - ${logMessage.message}"
            forwarder.forwardMessage(logMessage.toRust(), formattedMessage)
            logger.i { formattedMessage }
        } catch (e: Exception) {
            logger.e(e) { "Error forwarding log message: ${logMessage.message}" }
        }
    }

    /**
     * Clear all pending log messages from the Rust logger queue.
     */
    fun clearLogQueue() {
        try {
            com.yral.shared.uniffi.generated
                .clearLogMessages()
            logger.i { "Cleared log message queue" }
        } catch (e: LoggerException) {
            logger.e { "Error clearing log queue: $e" }
        } catch (e: Exception) {
            logger.e(e) { "Unexpected error clearing log queue" }
        }
    }

    /**
     * Get the current status of the forwarding service.
     */
    fun isForwarding(): Boolean = isRunning
}

interface RustLogForwardingListener {
    fun forwardMessage(
        logMessage: RustLogMessage,
        formattedMessage: String,
    )
}

data class RustLogMessage(
    val tag: String,
    val level: RustLogLevel,
    val message: String,
    val timestamp: ULong,
)

enum class RustLogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
}

fun LogMessage.toRust(): RustLogMessage =
    RustLogMessage(
        tag = tag,
        level =
            when (level) {
                LogLevel.ERROR -> RustLogLevel.ERROR
                LogLevel.WARN -> RustLogLevel.WARN
                LogLevel.INFO -> RustLogLevel.INFO
                LogLevel.DEBUG -> RustLogLevel.DEBUG
                LogLevel.TRACE -> RustLogLevel.TRACE
            },
        message = message,
        timestamp = timestamp,
    )
