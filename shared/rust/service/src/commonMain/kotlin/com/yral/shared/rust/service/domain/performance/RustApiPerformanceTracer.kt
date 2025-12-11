package com.yral.shared.rust.service.domain.performance

import co.touchlab.kermit.Logger
import kotlin.time.measureTimedValue

interface RustApiPerformanceTracer {
    fun createTrace(operationName: String): RustApiTrace
}

suspend inline fun <T> traceApiCall(
    tracer: RustApiPerformanceTracer,
    operationName: String,
    crossinline block: suspend () -> T,
): T {
    val trace = tracer.createTrace(operationName)
    return try {
        trace.start()
        val (result, time) = measureTimedValue { block() }
        trace.success()
        Logger.d("OnChainCall") { "$operationName $time" }
        result
    } catch (
        @Suppress("TooGenericExceptionCaught")
        e: Exception,
    ) {
        trace.error()
        throw e
    }
}
