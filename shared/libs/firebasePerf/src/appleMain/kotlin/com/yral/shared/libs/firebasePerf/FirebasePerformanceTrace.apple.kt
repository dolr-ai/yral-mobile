package com.yral.shared.libs.firebasePerf

import dev.gitlive.firebase.perf.metrics.Trace
import dev.gitlive.firebase.perf.metrics.ios
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun Trace.setAttribute(
    attribute: String,
    value: String,
) {
    ios?.setValue(value, attribute)
}
