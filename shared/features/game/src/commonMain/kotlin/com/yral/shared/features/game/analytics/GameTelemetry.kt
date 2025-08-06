package com.yral.shared.features.game.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.GameConcludedBottomsheetClickedEventData
import com.yral.shared.analytics.events.GameConcludedCtaType
import com.yral.shared.analytics.events.GamePlayedEventData
import com.yral.shared.analytics.events.GameResult
import com.yral.shared.analytics.events.GameTutorialShownEventData
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.GameVotedEventData
import com.yral.shared.analytics.events.TokenType
import com.yral.shared.rust.domain.models.FeedDetails

class GameTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onGameVoted(
        feedDetails: FeedDetails,
        lossPenalty: Int,
        optionChosen: String,
        isTutorialVote: Boolean,
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
                    stakeType = TokenType.YRAL,
                    stakeAmount = lossPenalty,
                    optionChosen = optionChosen,
                    isTutorialVote = isTutorialVote,
                ),
        )
    }

    fun onGamePlayed(
        feedDetails: FeedDetails,
        lossPenalty: Int,
        optionChosen: String,
        coinDelta: Int,
        isTutorialVote: Boolean,
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
                    stakeType = TokenType.YRAL,
                    stakeAmount = lossPenalty,
                    optionChosen = optionChosen,
                    gameResult = if (coinDelta > 0) GameResult.WIN else GameResult.LOSS,
                    wonLossAmount = coinDelta,
                    isTutorialVote = isTutorialVote,
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
                    stakeType = TokenType.YRAL,
                    stakeAmount = lossPenalty,
                    gameResult = if (coinDelta > 0) GameResult.WIN else GameResult.LOSS,
                    wonLossAmount = coinDelta,
                    ctaType = ctaType,
                ),
        )
    }

    fun onGameTutorialShown(feedDetails: FeedDetails) {
        analyticsManager.trackEvent(
            event =
                GameTutorialShownEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.isNSFW(),
                    shareCount = 0,
                    gameType = GameType.SMILEY,
                ),
        )
    }
}
