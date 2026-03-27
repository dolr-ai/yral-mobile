package com.yral.shared.app.notifications

import com.yral.shared.libs.designsystem.component.toast.ToastCTA
import com.yral.shared.libs.designsystem.component.toast.ToastDuration
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showSuccess

fun showForegroundNotificationSuccessToast(
    title: String?,
    body: String?,
) {
    val toastType = buildForegroundNotificationToastType(title, body) ?: return
    ToastManager.showToast(
        type = toastType,
        status = ToastStatus.Success,
    )
}

fun showForegroundNotificationSuccessToastWithAction(
    title: String?,
    body: String?,
    actionText: String,
    onTap: () -> Unit,
) {
    val toastType = buildForegroundNotificationToastType(title, body) ?: return
    ToastManager.showSuccess(
        type = toastType,
        cta =
            ToastCTA(
                text = actionText,
                onClick = onTap,
            ),
        duration = ToastDuration.LONG,
    )
}

private fun buildForegroundNotificationToastType(
    title: String?,
    body: String?,
): ToastType? =
    if (title != null && body != null) {
        ToastType.Big(
            heading = title,
            message = body,
        )
    } else {
        val message = title ?: body
        if (message != null) {
            ToastType.Small(message = message)
        } else {
            null
        }
    }
