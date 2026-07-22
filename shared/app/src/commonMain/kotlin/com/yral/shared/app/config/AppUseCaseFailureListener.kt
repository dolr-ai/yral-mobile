package com.yral.shared.app.config

import com.yral.shared.core.utils.safeValueOf
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.http.exception.hasNetworkCause
import com.yral.shared.libs.arch.domain.UseCaseFailureListener

internal class AppUseCaseFailureListener(
    private val crashlyticsManager: CrashlyticsManager,
) : UseCaseFailureListener {
    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
        exceptionType: String?,
    ) {
        val type =
            if (throwable.hasNetworkCause()) {
                ExceptionType.NETWORK
            } else {
                safeValueOf<ExceptionType>(exceptionType?.uppercase()) ?: ExceptionType.UNKNOWN
            }
        crashlyticsManager.recordException(
            exception = Exception(throwable),
            type = type,
        )
    }
}
