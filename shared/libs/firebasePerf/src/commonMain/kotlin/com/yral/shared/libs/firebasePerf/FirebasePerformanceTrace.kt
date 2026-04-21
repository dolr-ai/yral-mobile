package com.yral.shared.libs.firebasePerf

/**
 * Firebase Performance trace backed by the platform Firebase SDK.
 */
expect open class FirebasePerformanceTrace(
    traceName: String,
) : PerformanceTrace {
    override fun start()

    override fun stop()

    override fun incrementMetric(
        metricName: String,
        incrementBy: Long,
    )

    override fun putAttribute(
        attribute: String,
        value: String,
    )

    override fun putMetric(
        metricName: String,
        value: Long,
    )
}
