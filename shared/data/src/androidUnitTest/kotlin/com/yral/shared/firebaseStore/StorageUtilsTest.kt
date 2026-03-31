package com.yral.shared.firebaseStore

import dev.gitlive.firebase.storage.FirebaseStorage
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageUtilsTest {
    @Test
    fun getDownloadUrlReturnsEmptyPathWithoutTouchingStorage() =
        runTest {
            val storage = mockk<FirebaseStorage>()

            val result = getDownloadUrl("", storage)

            assertEquals("", result)
            confirmVerified(storage)
        }
}
