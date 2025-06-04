package com.yral.shared.libs.firebasePerf

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.perf.metrics.Trace
import dev.gitlive.firebase.perf.performance

/**
 * Android Firebase Performance implementation
 * This provides Firebase Performance monitoring capabilities for Android platform
 */
open class FirebasePerformanceMonitor(
    traceName: String,
) : PerformanceMonitor {
    private var trace: Trace? = null

    init {
        trace = Firebase.performance.newTrace(traceName)
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
