package com.yral.shared.app.notifications

import com.yral.shared.libs.designsystem.component.toast.ToastDuration
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForegroundNotificationToastBridgeTest {
    @AfterTest
    fun tearDown() {
        ToastManager.clear()
    }

    @Test
    fun `showSuccess enqueues success toast without cta`() {
        showForegroundNotificationSuccessToast(
            title = "Video Published",
            body = "Your video is now live",
        )

        val toast = ToastManager.toastQueue.value.single()
        assertEquals(ToastStatus.Success, toast.status)
        assertNull(toast.cta)
        assertEquals(ToastDuration.LONG, toast.duration)
        assertEquals("Video Published", (toast.type as ToastType.Big).heading)
        assertEquals("Your video is now live", toast.type.message)
    }

    @Test
    fun `showSuccessWithAction enqueues success toast with cta`() {
        var tapped = false

        showForegroundNotificationSuccessToastWithAction(
            title = "Draft Ready",
            body = "Your video draft is ready",
            actionText = "View Drafts",
            onTap = { tapped = true },
        )

        val toast = ToastManager.toastQueue.value.single()
        assertEquals(ToastStatus.Success, toast.status)
        assertEquals(ToastDuration.LONG, toast.duration)
        assertEquals("Draft Ready", (toast.type as ToastType.Big).heading)
        val cta = toast.cta
        assertNotNull(cta)
        assertEquals("View Drafts", cta.text)
        cta.onClick()
        assertTrue(tapped)
    }

    @Test
    fun `showSuccess ignores empty title and body`() {
        showForegroundNotificationSuccessToast(
            title = null,
            body = null,
        )

        assertFalse(ToastManager.toastQueue.value.isNotEmpty())
    }
}
