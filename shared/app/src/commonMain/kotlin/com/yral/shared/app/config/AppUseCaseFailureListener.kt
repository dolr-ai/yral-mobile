package com.yral.shared.app.config

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.libs.arch.domain.UseCaseFailureListener

internal class AppUseCaseFailureListener(
    private val crashlyticsManager: CrashlyticsManager,
) : UseCaseFailureListener {
    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
    ) {
        crashlyticsManager.recordException(Exception("$tag: ${message()}", throwable))
    }
}
