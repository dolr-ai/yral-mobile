package com.yral.shared.rust.service.services

import co.touchlab.kermit.Logger
import com.yral.shared.uniffi.generated.LogMessage
import com.yral.shared.uniffi.generated.LoggerException
import com.yral.shared.uniffi.generated.getLogMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
@Suppress("TooGenericExceptionCaught", "UnusedParameter")
class LogForwardingService {
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
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
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

            // Forward to Crashlytics
            forwardToCrashlytics(logMessage, formattedMessage)

            // Forward to Sentry (uncomment if using Sentry)
            // forwardToSentry(logMessage, formattedMessage)
        } catch (e: Exception) {
            logger.e(e) { "Error forwarding log message: ${logMessage.message}" }
        }
    }

    /**
     * Forward log message to Firebase Crashlytics.
     */
    private fun forwardToCrashlytics(
        logMessage: LogMessage,
        formattedMessage: String,
    ) {
        try {
            // Uncomment and implement when Firebase Crashlytics is available

            /*
            val crashlytics = FirebaseCrashlytics.getInstance()

            // Set custom keys for better filtering
            crashlytics.setCustomKey("rust_log_level", logMessage.level.name)
            crashlytics.setCustomKey("rust_log_tag", logMessage.tag)
            crashlytics.setCustomKey("rust_log_timestamp", logMessage.timestamp.toString())

            // Log the message
            crashlytics.log(formattedMessage)

            // For error level logs, also record as non-fatal exception
            if (logMessage.level == com.yral.shared.uniffi.generated.LogLevel.Error) {
                crashlytics.recordException(
                    Exception("Rust Error: ${logMessage.message}")
                )
            }
             */

            // For now, just log to console
            logger.i { "Crashlytics: $formattedMessage" }
        } catch (e: Exception) {
            logger.e(e) { "Error forwarding to Crashlytics" }
        }
    }

    /**
     * Forward log message to Sentry.
     */
    @Suppress("UnusedPrivateMember")
    private fun forwardToSentry(
        logMessage: LogMessage,
        formattedMessage: String,
    ) {
        try {
            // Uncomment and implement when Sentry is available

            /*
            val sentryLevel = when (logMessage.level) {
                com.yral.shared.uniffi.generated.LogLevel.Error -> SentryLevel.ERROR
                com.yral.shared.uniffi.generated.LogLevel.Warn -> SentryLevel.WARNING
                com.yral.shared.uniffi.generated.LogLevel.Info -> SentryLevel.INFO
                com.yral.shared.uniffi.generated.LogLevel.Debug -> SentryLevel.DEBUG
                com.yral.shared.uniffi.generated.LogLevel.Trace -> SentryLevel.DEBUG
            }

            SentrySDK.captureMessage(formattedMessage) { scope ->
                scope.level = sentryLevel
                scope.setTag("rust_log_tag", logMessage.tag)
                scope.setTag("rust_log_level", logMessage.level.name)
                scope.setContext("rust_log", mapOf(
                    "timestamp" to logMessage.timestamp,
                    "tag" to logMessage.tag,
                    "level" to logMessage.level.name,
                    "message" to logMessage.message
                ))
            }
             */

            // For now, just log to console
            logger.i { "Sentry: $formattedMessage" }
        } catch (e: Exception) {
            logger.e(e) { "Error forwarding to Sentry" }
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
