package com.yral.shared.rust.service.domain.performance

import com.yral.shared.libs.firebasePerf.FirebasePerformanceTrace
import com.yral.shared.libs.firebasePerf.PerformanceConstants

class RustApiTrace(
    operationName: String,
) : FirebasePerformanceTrace("rust_api_$operationName") {
    init {
        putAttribute(PerformanceConstants.MODULE_KEY, "rust_service")
        putAttribute(PerformanceConstants.OPERATION_KEY, operationName)
    }

    fun success() {
        putAttribute(PerformanceConstants.RESULT_KEY, PerformanceConstants.SUCCESS_VALUE)
        stop()
    }

    fun error() {
        putAttribute(PerformanceConstants.RESULT_KEY, PerformanceConstants.ERROR_VALUE)
        stop()
    }
}
