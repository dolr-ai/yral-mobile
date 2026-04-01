package com.yral.shared.features.wallet.ui

internal sealed interface WalletAccessState {
    data object Unlocked : WalletAccessState

    data class Locked(
        val subtitle: WalletLockedSubtitle,
        val cta: WalletLockedCta,
    ) : WalletAccessState
}

internal enum class WalletLockedSubtitle {
    Default,
    SwitchProfile,
}

internal enum class WalletLockedCta {
    CreateInfluencer,
    SwitchProfile,
}

internal fun resolveWalletAccessState(
    isSocialSignedIn: Boolean,
    hasBots: Boolean,
    isBotAccount: Boolean,
): WalletAccessState =
    when {
        isSocialSignedIn && hasBots && isBotAccount -> WalletAccessState.Unlocked
        isSocialSignedIn && hasBots ->
            WalletAccessState.Locked(
                subtitle = WalletLockedSubtitle.SwitchProfile,
                cta = WalletLockedCta.SwitchProfile,
            )
        else ->
            WalletAccessState.Locked(
                subtitle = WalletLockedSubtitle.Default,
                cta = WalletLockedCta.CreateInfluencer,
            )
    }
