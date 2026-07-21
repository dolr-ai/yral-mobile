package com.yral.shared.app.config

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.CrashlyticsProvider
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.http.exception.NetworkException
import kotlin.test.Test
import kotlin.test.assertEquals

class AppUseCaseFailureListenerTest {
    @Test
    fun onFailure_marksNetworkExceptionAsNetwork() {
        val provider = RecordingCrashlyticsProvider()
        val listener = AppUseCaseFailureListener(CrashlyticsManager(listOf(provider)))

        listener.onFailure(
            throwable = NetworkException(RuntimeException("server error")),
            tag = "test",
            message = { "failed" },
            exceptionType = null,
        )

        assertEquals(ExceptionType.NETWORK, provider.recordedTypes.single())
    }

    @Test
    fun onFailure_marksNestedNetworkCauseAsNetwork() {
        val provider = RecordingCrashlyticsProvider()
        val listener = AppUseCaseFailureListener(CrashlyticsManager(listOf(provider)))

        listener.onFailure(
            throwable = Exception("wrapper", NetworkException(RuntimeException("server error"))),
            tag = "test",
            message = { "failed" },
            exceptionType = "auth",
        )

        assertEquals(ExceptionType.NETWORK, provider.recordedTypes.single())
    }

    @Test
    fun onFailure_usesProvidedExceptionTypeForNonNetworkFailures() {
        val provider = RecordingCrashlyticsProvider()
        val listener = AppUseCaseFailureListener(CrashlyticsManager(listOf(provider)))

        listener.onFailure(
            throwable = RuntimeException("boom"),
            tag = "test",
            message = { "failed" },
            exceptionType = "auth",
        )

        assertEquals(ExceptionType.AUTH, provider.recordedTypes.single())
    }

    @Test
    fun onFailure_defaultsToUnknownWithoutExceptionType() {
        val provider = RecordingCrashlyticsProvider()
        val listener = AppUseCaseFailureListener(CrashlyticsManager(listOf(provider)))

        listener.onFailure(
            throwable = RuntimeException("boom"),
            tag = "test",
            message = { "failed" },
            exceptionType = null,
        )

        assertEquals(ExceptionType.UNKNOWN, provider.recordedTypes.single())
    }

    private class RecordingCrashlyticsProvider : CrashlyticsProvider {
        override val name: String = "recording"
        val recordedTypes = mutableListOf<ExceptionType>()

        override fun recordException(exception: Exception) = Unit

        override fun recordException(
            exception: Exception,
            type: ExceptionType,
        ) {
            recordedTypes += type
        }

        override fun logMessage(message: String) = Unit

        override fun setUserId(id: String) = Unit
    }
}
