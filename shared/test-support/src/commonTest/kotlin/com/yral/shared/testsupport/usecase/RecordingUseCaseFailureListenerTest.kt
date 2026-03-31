package com.yral.shared.testsupport.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingUseCaseFailureListenerTest {
    @Test
    fun storesLastFailure() {
        val listener = RecordingUseCaseFailureListener()
        val failure = IllegalStateException("boom")

        listener.onFailure(failure, tag = null, message = { "ignored" }, exceptionType = null)

        assertEquals(failure, listener.lastFailure)
    }
}
