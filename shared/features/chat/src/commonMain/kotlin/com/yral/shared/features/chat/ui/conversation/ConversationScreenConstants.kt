package com.yral.shared.features.chat.ui.conversation

import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal const val MESSAGE_MAX_WIDTH_RATIO = 0.85f
internal const val SCROLL_OFFSET_RATIO = 0.8f
internal val MESSAGE_VERTICAL_PADDING_DP = 6.dp
internal const val ARROW_ROTATION = 180f
internal val CHAT_MEDIA_MIN_HEIGHT = 160.dp
internal val CHAT_MEDIA_MAX_HEIGHT = 360.dp
internal const val CHAT_MEDIA_DEFAULT_ASPECT_RATIO = 4f / 3f

internal data class ChatMediaContainerSize(
    val widthPx: Int,
    val heightPx: Int,
)

internal fun resolveChatMediaContainerSize(
    maxWidthPx: Float,
    imageAspectRatio: Float?,
    minHeightPx: Float,
    maxHeightPx: Float,
): ChatMediaContainerSize {
    val safeAspectRatio = normalizeChatMediaAspectRatio(imageAspectRatio)
    val safeMinHeightPx = minHeightPx.coerceAtMost(maxHeightPx)

    if (!maxWidthPx.isFinite() || maxWidthPx <= 0f) {
        return ChatMediaContainerSize(
            widthPx = safeMinHeightPx.roundToInt(),
            heightPx = safeMinHeightPx.roundToInt(),
        )
    }

    var targetWidthPx = maxWidthPx
    var targetHeightPx = targetWidthPx / safeAspectRatio

    if (targetHeightPx > maxHeightPx) {
        targetHeightPx = maxHeightPx
        targetWidthPx = targetHeightPx * safeAspectRatio
    } else if (targetHeightPx < safeMinHeightPx) {
        targetHeightPx = safeMinHeightPx
        targetWidthPx = targetHeightPx * safeAspectRatio

        if (targetWidthPx > maxWidthPx) {
            targetWidthPx = maxWidthPx
            targetHeightPx = targetWidthPx / safeAspectRatio
        }
    }

    return ChatMediaContainerSize(
        widthPx = targetWidthPx.roundToInt().coerceAtLeast(1),
        heightPx = targetHeightPx.roundToInt().coerceAtLeast(1),
    )
}

internal fun resolveChatMediaContainerHeight(
    containerWidthPx: Float,
    imageAspectRatio: Float?,
    minHeightPx: Float,
    maxHeightPx: Float,
): Float =
    resolveChatMediaContainerSize(
        maxWidthPx = containerWidthPx,
        imageAspectRatio = imageAspectRatio,
        minHeightPx = minHeightPx,
        maxHeightPx = maxHeightPx,
    ).heightPx.toFloat()

internal fun normalizeChatMediaAspectRatio(imageAspectRatio: Float?): Float =
    imageAspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?: CHAT_MEDIA_DEFAULT_ASPECT_RATIO

internal fun resolveChatMediaAspectRatio(
    imageWidthPx: Float,
    imageHeightPx: Float,
): Float? =
    if (isValidChatMediaDimension(imageWidthPx) && isValidChatMediaDimension(imageHeightPx)) {
        imageWidthPx / imageHeightPx
    } else {
        null
    }

private fun isValidChatMediaDimension(value: Float): Boolean = value.isFinite() && value > 0f
