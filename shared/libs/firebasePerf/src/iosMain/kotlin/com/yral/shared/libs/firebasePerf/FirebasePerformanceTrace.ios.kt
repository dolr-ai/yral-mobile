@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yral.shared.libs.firebasePerf

import cocoapods.FirebasePerformance.FIRPerformance
import cocoapods.FirebasePerformance.FIRTrace

actual open class FirebasePerformanceTrace actual constructor(
    traceName: String,
) : PerformanceTrace {
    private val trace: FIRTrace? = FIRPerformance.sharedInstance().traceWithName(traceName)

    actual override fun start() {
        trace?.start()
    }

    actual override fun stop() {
        trace?.stop()
    }

    actual override fun incrementMetric(
        metricName: String,
        incrementBy: Long,
    ) {
        trace?.incrementMetric(metricName, byInt = incrementBy)
    }

    actual override fun putAttribute(
        attribute: String,
        value: String,
    ) {
        trace?.setValue(value, forAttribute = attribute)
    }

    actual override fun putMetric(
        metricName: String,
        value: Long,
    ) {
        trace?.setIntValue(value, forMetric = metricName)
    }
}
