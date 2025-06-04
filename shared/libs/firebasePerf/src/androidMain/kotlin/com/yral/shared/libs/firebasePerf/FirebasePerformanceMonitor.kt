package com.yral.shared.libs.firebasePerf

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * Android Firebase Performance implementation
 * This provides Firebase Performance monitoring capabilities for Android platform
 */
open class FirebasePerformanceMonitor(
    traceName: String,
) : PerformanceMonitor {
    private var trace: Trace? = null

    init {
        trace = FirebasePerformance.getInstance().newTrace(traceName)
    }

    override fun start() {
        trace?.start()
    }

    override fun stop() {
        trace?.stop()
    }

    override fun incrementMetric(
        metricName: String,
        incrementBy: Long,
    ) {
        trace?.incrementMetric(metricName, incrementBy)
    }

    override fun putAttribute(
        attribute: String,
        value: String,
    ) {
        trace?.putAttribute(attribute, value)
    }

    override fun putMetric(
        metricName: String,
        value: Long,
    ) {
        trace?.putMetric(metricName, value)
    }
}

/**
 * Base class for operation traces with common functionality
 */
abstract class OperationTrace(
    traceName: String,
) : FirebasePerformanceMonitor(traceName) {
    /**
     * Mark trace as successful and stop it
     */
    fun success() {
        putAttribute(PerformanceConstants.RESULT_KEY, PerformanceConstants.SUCCESS_VALUE)
        stop()
    }

    /**
     * Mark trace as failed and stop it
     */
    fun error() {
        putAttribute(PerformanceConstants.RESULT_KEY, PerformanceConstants.ERROR_VALUE)
        stop()
    }

    /**
     * Set a module identifier for this trace
     */
    fun setModule(module: String) {
        putAttribute(PerformanceConstants.MODULE_KEY, module)
    }

    /**
     * Set an operation identifier for this trace
     */
    fun setOperation(operation: String) {
        putAttribute(PerformanceConstants.OPERATION_KEY, operation)
    }
}
