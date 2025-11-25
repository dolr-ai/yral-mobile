package com.yral.shared.app.config

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.libs.arch.domain.UseCaseExceptionType
import com.yral.shared.libs.arch.domain.UseCaseExceptionTypeMapper
import com.yral.shared.libs.arch.domain.UseCaseFailureListener

internal class AppUseCaseFailureListener(
    private val crashlyticsManager: CrashlyticsManager,
    private val exceptionTypeMapper: UseCaseExceptionTypeMapper,
) : UseCaseFailureListener {
    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
        exceptionType: UseCaseExceptionType?,
    ) {
        val type = exceptionTypeMapper.map(exceptionType ?: UseCaseExceptionType.Unknown)
        crashlyticsManager.recordException(
            exception = Exception(throwable),
            type = (type as? ExceptionType) ?: ExceptionType.UNKNOWN,
        )
    }
}
