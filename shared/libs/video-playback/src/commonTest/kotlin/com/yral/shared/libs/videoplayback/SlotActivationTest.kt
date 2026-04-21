package com.yral.shared.libs.videoplayback

import kotlin.test.Test
import kotlin.test.assertEquals

class SlotActivationTest {
    @Test
    fun `matching prepared ready slot swaps instead of preparing again`() {
        val decision =
            selectSlotActivationDecision(
                requestedIndex = 4,
                preparedIndex = 4,
                preparedReady = true,
            )

        assertEquals(SlotActivationMode.SwapPrepared, decision.mode)
        assertEquals("prepared_swap", decision.playStartReason)
    }

    @Test
    fun `matching prepared slot that is not ready falls back to active prepare`() {
        val decision =
            selectSlotActivationDecision(
                requestedIndex = 4,
                preparedIndex = 4,
                preparedReady = false,
            )

        assertEquals(SlotActivationMode.PrepareActive, decision.mode)
        assertEquals("prepared_not_ready", decision.playStartReason)
    }

    @Test
    fun `different prepared slot index uses active prepare path`() {
        val decision =
            selectSlotActivationDecision(
                requestedIndex = 4,
                preparedIndex = 5,
                preparedReady = true,
            )

        assertEquals(SlotActivationMode.PrepareActive, decision.mode)
        assertEquals("active_prepare", decision.playStartReason)
    }
}
