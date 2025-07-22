package com.yral.shared.features.game.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.GameConcludedBottomsheetClickedEventData
import com.yral.shared.analytics.events.GameConcludedCtaType
import com.yral.shared.analytics.events.GamePlayedEventData
import com.yral.shared.analytics.events.GameResult
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.GameVotedEventData
import com.yral.shared.analytics.events.StakeType
import com.yral.shared.rust.domain.models.FeedDetails

class GameTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onGameVoted(
        feedDetails: FeedDetails,
        lossPenalty: Int,
        optionChosen: String,
    ) {
        analyticsManager.trackEvent(
            event =
                GameVotedEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.isNSFW(),
                    shareCount = 0,
                    gameType = GameType.SMILEY,
                    stakeType = StakeType.SATS,
                    stakeAmount = lossPenalty,
                    optionChosen = optionChosen,
                ),
        )
    }

    fun onGamePlayed(
        feedDetails: FeedDetails,
        lossPenalty: Int,
        optionChosen: String,
        coinDelta: Int,
    ) {
        analyticsManager.trackEvent(
            event =
                GamePlayedEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.isNSFW(),
                    shareCount = 0,
                    gameType = GameType.SMILEY,
                    stakeType = StakeType.SATS,
                    stakeAmount = lossPenalty,
                    optionChosen = optionChosen,
                    gameResult = if (coinDelta > 0) GameResult.WIN else GameResult.LOSS,
                    wonLossAmount = coinDelta,
                ),
        )
    }

    fun gameConcludedBottomSheetClicked(
        lossPenalty: Int,
        coinDelta: Int,
        ctaType: GameConcludedCtaType,
    ) {
        analyticsManager.trackEvent(
            event =
                GameConcludedBottomsheetClickedEventData(
                    stakeType = StakeType.SATS,
                    stakeAmount = lossPenalty,
                    gameResult = if (coinDelta > 0) GameResult.WIN else GameResult.LOSS,
                    wonLossAmount = coinDelta,
                    ctaType = ctaType,
                ),
        )
    }
}
