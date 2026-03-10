package com.yral.shared.features.game.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmileyGameTest {
    @Test
    fun shouldRenderSmileyGameOverlayReturnsTrueForCurrentSmileyPage() {
        assertTrue(
            shouldRenderSmileyGameOverlay(
                pageNo = 4,
                currentPage = 4,
                isCardLayoutEnabled = false,
            ),
        )
    }

    @Test
    fun shouldRenderSmileyGameOverlayReturnsFalseForOffscreenSmileyPage() {
        assertFalse(
            shouldRenderSmileyGameOverlay(
                pageNo = 3,
                currentPage = 4,
                isCardLayoutEnabled = false,
            ),
        )
    }

    @Test
    fun shouldRenderSmileyGameOverlayReturnsFalseForCardLayout() {
        assertFalse(
            shouldRenderSmileyGameOverlay(
                pageNo = 4,
                currentPage = 4,
                isCardLayoutEnabled = true,
            ),
        )
    }
}
