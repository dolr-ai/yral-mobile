package com.yral.shared.libs.firebasePerf

/**
 * Base class for operation traces with common functionality
 */
abstract class FirebaseOperationTrace(
    traceName: String,
) : FirebasePerformanceTrace(traceName) {
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
