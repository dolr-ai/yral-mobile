package com.yral.shared.app.nav

import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Config {
    @Serializable
    data object Splash : Config

    @Serializable
    data object Home : Config

    @Serializable
    data object EditProfile : Config

    @Serializable
    data class UserProfile(
        val userCanisterData: CanisterData,
    ) : Config

    @Serializable
    data class TournamentLeaderboard(
        val tournamentId: String,
        val showResult: Boolean = false,
    ) : Config

    @Serializable
    data class TournamentGame(
        val tournamentId: String,
        val tournamentTitle: String = "",
        val initialDiamonds: Int,
        val startEpochMs: Long,
        val endEpochMs: Long,
        val totalPrizePool: Int,
        val isHotOrNot: Boolean = false,
    ) : Config

    @Serializable
    data class Conversation(
        val influencerId: String,
        val influencerCategory: String,
        val influencerSource: InfluencerSource = InfluencerSource.CARD,
    ) : Config

    @Serializable
    data object Wallet : Config

    @Serializable
    data object Leaderboard : Config

    @Serializable
    data object CountrySelector : Config

    @Serializable
    data object OtpVerification : Config

    @Serializable
    data object MandatoryLogin : Config

    @Serializable
    data object Subscription : Config
}
