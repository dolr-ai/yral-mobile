package com.yral.android

import com.yral.shared.libs.designsystem.component.toast.ToastDuration
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForegroundNotificationHandlerTest {
    private lateinit var handler: ForegroundNotificationHandler
    private var lastNavigatedPayload: String? = null

    @BeforeTest
    fun setup() {
        ToastManager.clear()
        lastNavigatedPayload = null
        handler = ForegroundNotificationHandler(viewDraftsCtaText = "View Drafts")
    }

    @AfterTest
    fun tearDown() {
        ToastManager.clear()
    }

    private fun onNavigate(payload: String) {
        lastNavigatedPayload = payload
    }

    // --- Message 1: VideoUploadedToDraft (no payload key, top-level data) ---

    @Test
    fun `VideoUploadedToDraft without payload wraps data and shows toast with CTA`() {
        val data =
            mapOf(
                "post_id" to "e85287ee-c7be-42f1-b07f-4c58d1d51c34",
                "user_principal" to "z22gk-nbsca",
                "type" to "VideoUploadedToDraft",
            )

        handler.handle(
            title = "Draft Ready",
            body = "Your video draft is ready",
            data = data,
            onNavigate = ::onNavigate,
        )

        val toasts = ToastManager.toastQueue.value
        assertEquals(1, toasts.size)
        val toast = toasts.first()
        assertIs<ToastType.Big>(toast.type)
        assertEquals("Draft Ready", (toast.type as ToastType.Big).heading)
        assertEquals("Your video draft is ready", toast.type.message)
        assertEquals(ToastStatus.Success, toast.status)
        assertNotNull(toast.cta)
        assertEquals("View Drafts", toast.cta!!.text)
        assertEquals(ToastDuration.LONG, toast.duration)

        // CTA navigates with a wrapped payload containing internalUrl
        toast.cta!!.onClick()
        assertNotNull(lastNavigatedPayload)
        val payload = Json.decodeFromString(JsonObject.serializer(), lastNavigatedPayload!!)
        assertEquals("VideoUploadedToDraft", payload["type"]?.jsonPrimitive?.content)
        assertEquals("profile", payload["internalUrl"]?.jsonPrimitive?.content)
    }

    // --- Message 2: VideoUploadSuccessful (has payload JSON with internalUrl) ---

    @Test
    fun `VideoUploadSuccessful with payload shows simple toast without navigation`() {
        val payloadJson =
            """{"type":"VideoUploadSuccessful","user_id":"z22gk","internalUrl":"profile/videos"}"""
        val data = mapOf("payload" to payloadJson)

        handler.handle(
            title = "Video Published",
            body = "Your video has been published successfully",
            data = data,
            onNavigate = ::onNavigate,
        )

        val toasts = ToastManager.toastQueue.value
        assertEquals(1, toasts.size)
        val toast = toasts.first()
        assertIs<ToastType.Big>(toast.type)
        assertEquals("Video Published", (toast.type as ToastType.Big).heading)
        assertEquals(ToastStatus.Success, toast.status)
        assertNull(toast.cta)
        assertNull(lastNavigatedPayload)
    }

    // --- Message 3: VideoPublished (no payload key, top-level data) ---

    @Test
    fun `VideoPublished without payload key shows simple toast`() {
        val data =
            mapOf(
                "post_id" to "e85287ee-c7be-42f1-b07f-4c58d1d51c34",
                "user_principal" to "z22gk-nbsca",
                "type" to "VideoPublished",
            )

        handler.handle(
            title = "Video Published",
            body = null,
            data = data,
            onNavigate = ::onNavigate,
        )

        val toasts = ToastManager.toastQueue.value
        assertEquals(1, toasts.size)
        val toast = toasts.first()
        assertIs<ToastType.Small>(toast.type)
        assertEquals("Video Published", toast.type.message)
        assertEquals(ToastStatus.Success, toast.status)
        assertNull(lastNavigatedPayload)
    }

    // --- DraftCreated with existing payload JSON ---

    @Test
    fun `VideoUploadedToDraft inside payload uses payload as-is for navigation`() {
        val payloadJson = """{"type":"VideoUploadedToDraft","post_id":"e85287ee","internalUrl":"profile"}"""
        val data = mapOf("payload" to payloadJson)

        handler.handle(
            title = "Draft Ready",
            body = "Your AI video draft is ready to review",
            data = data,
            onNavigate = ::onNavigate,
        )

        val toasts = ToastManager.toastQueue.value
        assertEquals(1, toasts.size)
        val toast = toasts.first()
        assertEquals(ToastStatus.Success, toast.status)
        assertNotNull(toast.cta)
        assertEquals("View Drafts", toast.cta!!.text)

        // CTA uses the original payload directly
        toast.cta!!.onClick()
        assertEquals(payloadJson, lastNavigatedPayload)
    }

    // --- RewardEarned navigates directly ---

    @Test
    fun `RewardEarned inside payload navigates without toast`() {
        val payloadJson = """{"type":"RewardEarned","internalUrl":"wallet/rewards"}"""
        val data = mapOf("payload" to payloadJson)

        handler.handle(
            title = "Reward Earned",
            body = "You earned 10 YRAL tokens",
            data = data,
            onNavigate = ::onNavigate,
        )

        assertTrue(ToastManager.isEmpty())
        assertEquals(payloadJson, lastNavigatedPayload)
    }

    // --- resolvePayload ---

    @Test
    fun `resolvePayload returns existing payload when present`() {
        val payloadJson = """{"type":"VideoUploadSuccessful"}"""
        val data = mapOf("payload" to payloadJson, "type" to "ignored")

        val result = handler.resolvePayload(data)
        assertEquals(payloadJson, result)
    }

    @Test
    fun `resolvePayload wraps data and adds internalUrl for draft type`() {
        val data = mapOf("type" to "VideoUploadedToDraft", "post_id" to "123")

        val result = handler.resolvePayload(data)
        val json = Json.decodeFromString(JsonObject.serializer(), result)
        assertEquals("VideoUploadedToDraft", json["type"]?.jsonPrimitive?.content)
        assertEquals("123", json["post_id"]?.jsonPrimitive?.content)
        assertEquals("profile", json["internalUrl"]?.jsonPrimitive?.content)
    }

    @Test
    fun `resolvePayload wraps data without internalUrl for non-draft type`() {
        val data = mapOf("type" to "VideoPublished", "post_id" to "456")

        val result = handler.resolvePayload(data)
        val json = Json.decodeFromString(JsonObject.serializer(), result)
        assertEquals("VideoPublished", json["type"]?.jsonPrimitive?.content)
        assertNull(json["internalUrl"])
    }

    // --- Edge cases ---

    @Test
    fun `no title and no body produces no toast`() {
        val data = mapOf("type" to "SomeNotification")

        handler.handle(
            title = null,
            body = null,
            data = data,
            onNavigate = ::onNavigate,
        )

        assertTrue(ToastManager.isEmpty())
        assertNull(lastNavigatedPayload)
    }

    @Test
    fun `title only produces Small toast`() {
        handler.handle(
            title = "Hello",
            body = null,
            data = emptyMap(),
            onNavigate = ::onNavigate,
        )

        val toast = ToastManager.toastQueue.value.first()
        assertIs<ToastType.Small>(toast.type)
        assertEquals("Hello", toast.type.message)
    }

    @Test
    fun `body only produces Small toast`() {
        handler.handle(
            title = null,
            body = "Some message",
            data = emptyMap(),
            onNavigate = ::onNavigate,
        )

        val toast = ToastManager.toastQueue.value.first()
        assertIs<ToastType.Small>(toast.type)
        assertEquals("Some message", toast.type.message)
    }
}
