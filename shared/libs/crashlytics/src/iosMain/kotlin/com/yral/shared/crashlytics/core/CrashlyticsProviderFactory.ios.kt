@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yral.shared.crashlytics.core

import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import cocoapods.FirebaseCrashlytics.FIRExceptionModel

private class NativeFirebaseCrashlyticsProvider(
    private val crashlytics: FIRCrashlytics,
) : CrashlyticsProvider {
    override val name: String
        get() = "firebase_native_ios"

    override fun recordException(exception: Exception) {
        recordException(
            exception = exception,
            type = ExceptionType.UNKNOWN,
        )
    }

    override fun recordException(
        exception: Exception,
        type: ExceptionType,
    ) {
        crashlytics.setCustomValue(type.name.lowercase(), "error_type")
        crashlytics.recordExceptionModel(exception.asExceptionModel())
    }

    override fun logMessage(message: String) {
        crashlytics.log(message)
    }

    override fun setUserId(id: String) {
        crashlytics.setUserID(id)
    }
}

internal actual fun createCrashlyticsProvider(): CrashlyticsProvider =
    NativeFirebaseCrashlyticsProvider(
        crashlytics = FIRCrashlytics.crashlytics(),
    )

private fun Throwable.asExceptionModel(): FIRExceptionModel =
    FIRExceptionModel(
        name = this::class.simpleName ?: "KotlinException",
        reason = message ?: toString(),
    )
