package com.yral.shared.rust.service.domain.performance

class FirebaseRustApiTracer : RustApiPerformanceTracer {
    override fun createTrace(operationName: String): RustApiTrace = RustApiTrace(operationName)
}
