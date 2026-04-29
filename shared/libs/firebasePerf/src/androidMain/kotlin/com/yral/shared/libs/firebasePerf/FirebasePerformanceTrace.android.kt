package com.yral.shared.libs.firebasePerf

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

actual open class FirebasePerformanceTrace actual constructor(
    traceName: String,
) : PerformanceTrace {
    private val trace: Trace = FirebasePerformance.getInstance().newTrace(traceName)

    actual override fun start() {
        trace.start()
    }

    actual override fun stop() {
        trace.stop()
    }

    actual override fun incrementMetric(
        metricName: String,
        incrementBy: Long,
    ) {
        trace.incrementMetric(metricName, incrementBy)
    }

    actual override fun putAttribute(
        attribute: String,
        value: String,
    ) {
        trace.putAttribute(attribute, value)
    }

    actual override fun putMetric(
        metricName: String,
        value: Long,
    ) {
        trace.putMetric(metricName, value)
    }
}
