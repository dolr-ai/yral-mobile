package com.yral.shared.features.profile.ui

import com.yral.shared.features.profile.viewmodel.ViewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileSubscribeButtonStateTest {
    @Test
    fun `subscribed influencer profiles keep the subscribe button visible`() {
        val state =
            ViewState(
                isOwnProfile = false,
                isAiInfluencer = true,
                isLoggedIn = true,
                isSubscribedToInfluencer = true,
            )

        val uiState = state.profileSubscribeButtonUiState()

        assertTrue(uiState.shouldShow)
        assertTrue(uiState.isSubscribed)
    }

    @Test
    fun `guest users do not see the subscribe button`() {
        val state =
            ViewState(
                isOwnProfile = false,
                isAiInfluencer = true,
                isLoggedIn = false,
                isSubscribedToInfluencer = false,
            )

        val uiState = state.profileSubscribeButtonUiState()

        assertFalse(uiState.shouldShow)
        assertFalse(uiState.isSubscribed)
    }

    @Test
    fun `own profiles never show the influencer subscribe button`() {
        val uiState =
            ViewState(
                isOwnProfile = true,
                isAiInfluencer = true,
                isLoggedIn = true,
                isSubscribedToInfluencer = true,
            ).profileSubscribeButtonUiState()

        assertEquals(
            ProfileSubscribeButtonUiState(
                shouldShow = false,
                isSubscribed = true,
            ),
            uiState,
        )
    }
}
