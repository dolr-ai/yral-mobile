package com.yral.shared.libs.firebasePerf

/**
 * Cross-platform Performance monitoring interface
 * This interface can be implemented by different platforms (Android, iOS) to provide
 * performance monitoring capabilities using their respective native libraries.
 */
interface PerformanceTrace {
    /**
     * Start the performance trace
     */
    fun start()

    /**
     * Stop the performance trace
     */
    fun stop()

    /**
     * Increment a metric by the specified value
     * @param metricName The name of the metric to increment
     * @param incrementBy The value to increment by
     */
    fun incrementMetric(
        metricName: String,
        incrementBy: Long,
    )

    /**
     * Set an attribute on the trace
     * @param attribute The attribute key
     * @param value The attribute value
     */
    fun putAttribute(
        attribute: String,
        value: String,
    )

    /**
     * Set a metric value
     * @param metricName The name of the metric
     * @param value The metric value
     */
    fun putMetric(
        metricName: String,
        value: Long,
    )
}

/**
 * Common performance monitoring constants
 */
object PerformanceConstants {
    // Common attribute keys
    const val RESULT_KEY = "result"
    const val URL_KEY = "url"
    const val MODULE_KEY = "module"
    const val OPERATION_KEY = "operation"

    // Common attribute values
    const val SUCCESS_VALUE = "success"
    const val ERROR_VALUE = "error"

    // Common metric names
    const val DURATION_MS = "duration_ms"
    const val COUNT_METRIC = "count"
}
