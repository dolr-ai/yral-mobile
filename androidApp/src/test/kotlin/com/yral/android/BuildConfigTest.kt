package com.yral.android

import kotlin.test.Test
import kotlin.test.assertTrue

class BuildConfigTest {
    @Test
    fun `generated build config exposes version metadata`() {
        assertTrue(BuildConfig.VERSION_CODE > 0)
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
    }
}
