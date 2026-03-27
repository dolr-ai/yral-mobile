package com.yral.android

import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.libs.designsystem.component.toast.ToastCTA
import com.yral.shared.libs.designsystem.component.toast.ToastDuration
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.routing.routes.api.Profile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

internal const val DRAFT_CREATED_TYPE = "VideoUploadedToDraft"
internal const val REWARD_EARNED_TYPE = "RewardEarned"

data class NotificationConfig(
    val fallbackInternalUrl: String? = null,
    val ctaText: String? = null,
    val navigateDirectly: Boolean = false,
)

internal fun notificationConfigByType(viewDraftsCtaText: String): Map<String, NotificationConfig> =
    mapOf(
        DRAFT_CREATED_TYPE to
            NotificationConfig(
                fallbackInternalUrl = Profile.PATH,
                ctaText = viewDraftsCtaText,
            ),
        REWARD_EARNED_TYPE to NotificationConfig(navigateDirectly = true),
    )

/**
 * Handles foreground notification messages by determining the appropriate toast or navigation action.
 * Extracted from [MyFirebaseMessagingService] for testability.
 */
class NotificationHandler(
    private val notificationConfigByType: Map<String, NotificationConfig>,
) {
    fun handle(
        title: String?,
        body: String?,
        data: Map<String, String>,
        onNavigate: (String) -> Unit,
    ) {
        val payload = resolvePayload(data)
        val type = extractTypeFromPayload(payload) ?: data["type"]
        val config = type?.let(notificationConfigByType::get)

        when {
            config?.navigateDirectly == true -> onNavigate(payload)
            type == DRAFT_CREATED_TYPE -> handleDraftCreated(title, body, payload, config, onNavigate)
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
        val type = fields["type"]?.content
        val config = type?.let(notificationConfigByType::get)
        if ("internalUrl" !in fields && config?.fallbackInternalUrl != null) {
            fields["internalUrl"] = JsonPrimitive(config.fallbackInternalUrl)
        }
        return Json.encodeToString(JsonObject.serializer(), JsonObject(fields))
    }

    internal fun resolvePayloadOrNull(data: Map<String, String>): String? {
        data["payload"]?.let { return it }

        val hasNotificationData =
            data["type"] != null ||
                data.containsKey("internalUrl") ||
                data.containsKey("post_id")

        return if (hasNotificationData) {
            resolvePayload(data)
        } else {
            null
        }
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
        config: NotificationConfig?,
        onNavigate: (String) -> Unit,
    ) {
        VideoGenerationTracker.onDraftCreatedAndRequestDraftsTab()
        val toastType = buildToastType(title, body) ?: return
        val ctaText = config?.ctaText ?: return
        ToastManager.showSuccess(
            type = toastType,
            cta =
                ToastCTA(
                    text = ctaText,
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
