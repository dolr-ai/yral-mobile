package com.yral.shared.libs.firebasePerf

import dev.gitlive.firebase.perf.metrics.Trace

actual fun Trace.setAttribute(
    attribute: String,
    value: String,
) {
    putAttribute(attribute, value)
}
