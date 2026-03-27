package com.yral.android

import com.yral.shared.libs.designsystem.component.toast.ToastCTA
import com.yral.shared.libs.designsystem.component.toast.ToastDuration
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

internal val NOTIF_THAT_REQUIRES_NAVIGATION = listOf("RewardEarned")
internal const val DRAFT_CREATED_TYPE = "VideoUploadedToDraft"
private const val DRAFTS_INTERNAL_URL = "profile"

/**
 * Handles foreground notification messages by determining the appropriate toast or navigation action.
 * Extracted from [MyFirebaseMessagingService] for testability.
 */
class ForegroundNotificationHandler(
    private val viewDraftsCtaText: String,
) {
    fun handle(
        title: String?,
        body: String?,
        data: Map<String, String>,
        onNavigate: (String) -> Unit,
    ) {
        val payload = resolvePayload(data)
        val type = extractTypeFromPayload(payload) ?: data["type"]

        when (type) {
            in NOTIF_THAT_REQUIRES_NAVIGATION -> onNavigate(payload)
            DRAFT_CREATED_TYPE -> handleDraftCreated(title, body, payload, onNavigate)
            else -> showSimpleToast(title, body)
        }
    }

    /**
     * Returns the payload JSON string. If `data["payload"]` exists, uses it directly.
     * Otherwise wraps `data` into a JSON payload, adding `internalUrl` for draft types.
     */
    internal fun resolvePayload(data: Map<String, String>): String {
        data["payload"]?.let { return it }

        val fields = data.mapValues { (_, v) -> JsonPrimitive(v) }.toMutableMap()
        if (fields["type"]?.content == DRAFT_CREATED_TYPE && "internalUrl" !in fields) {
            fields["internalUrl"] = JsonPrimitive(DRAFTS_INTERNAL_URL)
        }
        return Json.encodeToString(JsonObject.serializer(), JsonObject(fields))
    }

    private fun extractTypeFromPayload(payload: String): String? =
        try {
            val jsonObject = Json.decodeFromString(JsonObject.serializer(), payload)
            jsonObject["type"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }

    private fun showSimpleToast(
        title: String?,
        body: String?,
    ) {
        val toastType = buildToastType(title, body) ?: return
        ToastManager.showToast(
            type = toastType,
            status = ToastStatus.Success,
        )
    }

    private fun handleDraftCreated(
        title: String?,
        body: String?,
        payload: String,
        onNavigate: (String) -> Unit,
    ) {
        val toastType = buildToastType(title, body) ?: return
        ToastManager.showSuccess(
            type = toastType,
            cta =
                ToastCTA(
                    text = viewDraftsCtaText,
                    onClick = { onNavigate(payload) },
                ),
            duration = ToastDuration.LONG,
        )
    }

    private fun buildToastType(
        title: String?,
        body: String?,
    ): ToastType? =
        if (title != null && body != null) {
            ToastType.Big(title, body)
        } else {
            val message = title ?: body
            if (message != null) {
                ToastType.Small(message)
            } else {
                null
            }
        }
}
