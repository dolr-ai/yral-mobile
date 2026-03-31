package com.yral.shared.app.config

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.CrashlyticsProvider
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.http.exception.DNSLookupException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppHTTPEventListenerTest {
    @Test
    fun logException_marksAuthDnsFailuresAsAuth() {
        val provider = RecordingCrashlyticsProvider()
        val listener = AppHTTPEventListener(CrashlyticsManager(listOf(provider)))

        listener.logException(
            DNSLookupException(
                hostname = "auth.dolr.ai",
                lookupSource = "test_dns",
                cause = RuntimeException("dns failed"),
            ),
        )

        assertEquals(ExceptionType.AUTH, provider.recordedTypes.single())
        assertTrue(provider.loggedMessages.single().contains("error_type=auth"))
    }

    @Test
    fun logException_marksNonAuthDnsFailuresAsUnknown() {
        val provider = RecordingCrashlyticsProvider()
        val listener = AppHTTPEventListener(CrashlyticsManager(listOf(provider)))

        listener.logException(
            DNSLookupException(
                hostname = "metadata.yral.com",
                lookupSource = "test_dns",
                cause = RuntimeException("dns failed"),
            ),
        )

        assertEquals(ExceptionType.UNKNOWN, provider.recordedTypes.single())
        assertTrue(provider.loggedMessages.single().contains("error_type=unknown"))
    }

    private class RecordingCrashlyticsProvider : CrashlyticsProvider {
        override val name: String = "recording"
        val recordedTypes = mutableListOf<ExceptionType>()
        val loggedMessages = mutableListOf<String>()

        override fun recordException(exception: Exception) = Unit

        override fun recordException(
            exception: Exception,
            type: ExceptionType,
        ) {
            recordedTypes += type
        }

        override fun logMessage(message: String) {
            loggedMessages += message
        }

        override fun setUserId(id: String) = Unit
    }
}
