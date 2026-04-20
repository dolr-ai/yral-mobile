package com.yral.shared.libs.videoplayback

internal enum class SlotActivationMode {
    SwapPrepared,
    PrepareActive,
}

internal data class SlotActivationDecision(
    val mode: SlotActivationMode,
    val playStartReason: String,
)

internal fun selectSlotActivationDecision(
    requestedIndex: Int,
    preparedIndex: Int?,
    preparedReady: Boolean,
): SlotActivationDecision =
    when {
        preparedIndex == requestedIndex && preparedReady ->
            SlotActivationDecision(
                mode = SlotActivationMode.SwapPrepared,
                playStartReason = "prepared_swap",
            )

        preparedIndex == requestedIndex ->
            SlotActivationDecision(
                mode = SlotActivationMode.PrepareActive,
                playStartReason = "prepared_not_ready",
            )

        else ->
            SlotActivationDecision(
                mode = SlotActivationMode.PrepareActive,
                playStartReason = "active_prepare",
            )
    }
