package com.yral.shared.features.wallet.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class WalletAccessStateTest {
    @Test
    fun `signed in user without bots sees create influencer locked state`() {
        val accessState =
            resolveWalletAccessState(
                isSocialSignedIn = true,
                hasBots = false,
                isBotAccount = false,
            )

        assertEquals(
            WalletAccessState.Locked(
                subtitle = WalletLockedSubtitle.Default,
                cta = WalletLockedCta.CreateInfluencer,
            ),
            accessState,
        )
    }

    @Test
    fun `bot account with bots sees unlocked wallet`() {
        val accessState =
            resolveWalletAccessState(
                isSocialSignedIn = true,
                hasBots = true,
                isBotAccount = true,
            )

        assertEquals(WalletAccessState.Unlocked, accessState)
    }

    @Test
    fun `parent account with bots sees switch profile locked state`() {
        val accessState =
            resolveWalletAccessState(
                isSocialSignedIn = true,
                hasBots = true,
                isBotAccount = false,
            )

        assertEquals(
            WalletAccessState.Locked(
                subtitle = WalletLockedSubtitle.SwitchProfile,
                cta = WalletLockedCta.SwitchProfile,
            ),
            accessState,
        )
    }

    @Test
    fun `signed out users do not get switch profile prompt`() {
        val accessState =
            resolveWalletAccessState(
                isSocialSignedIn = false,
                hasBots = true,
                isBotAccount = false,
            )

        assertEquals(
            WalletAccessState.Locked(
                subtitle = WalletLockedSubtitle.Default,
                cta = WalletLockedCta.CreateInfluencer,
            ),
            accessState,
        )
    }
}
