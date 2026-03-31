package com.yral.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals

class RemovedFirebaseCloudFunctionsTest {
    @Test
    fun exceptionMessageIncludesOperationAndRemovalReason() {
        val exception = removedFirebaseCloudFunctionsException("castVote")
        val expectedText =
            "castVote is unavailable because Firebase Cloud Functions dependency has been removed from the project"

        assertEquals(expectedText, exception.text)
        assertEquals("Unknown exception: $expectedText", exception.message)
    }
}
