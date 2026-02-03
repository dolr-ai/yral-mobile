package com.yral.shared.features.game.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AirdropClaimedEventData
import com.yral.shared.analytics.events.ForcedGameplayNudgeShownEventData
import com.yral.shared.analytics.events.GameConcludedBottomsheetClickedEventData
import com.yral.shared.analytics.events.GameConcludedCtaType
import com.yral.shared.analytics.events.GamePlayedEventData
import com.yral.shared.analytics.events.GameResult
import com.yral.shared.analytics.events.GameTutorialShownEventData
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.GameVotedEventData
import com.yral.shared.analytics.events.TokenType
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.preferences.stores.AffiliateAttributionStore

class GameTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val affiliateAttributionStore: AffiliateAttributionStore,
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
                    affiliate = affiliateAttributionStore.peek(),
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

    fun onForcedGamePlayNudgeShown(feedDetails: FeedDetails) {
        analyticsManager.trackEvent(
            event =
                ForcedGameplayNudgeShownEventData(
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

    fun onAirdropClaimSuccess(claimedAmount: Int) {
        analyticsManager.trackEvent(
            AirdropClaimedEventData(
                tokenType = TokenType.YRAL,
                isSuccess = true,
                claimedAmount = claimedAmount,
                isAutoCredited = true,
            ),
        )
    }

    fun onAirdropClaimFailure() {
        analyticsManager.trackEvent(
            AirdropClaimedEventData(
                tokenType = TokenType.YRAL,
                isSuccess = false,
                claimedAmount = 0,
                isAutoCredited = false,
            ),
        )
    }

    fun onHotOrNotVoted(
        feedDetails: FeedDetails,
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
                    gameType = GameType.HOT_OR_NOT,
                    stakeType = TokenType.YRAL,
                    stakeAmount = HOT_OR_NOT_STAKE,
                    optionChosen = optionChosen,
                    isTutorialVote = false,
                ),
        )
    }

    fun onHotOrNotPlayed(
        feedDetails: FeedDetails,
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
                    gameType = GameType.HOT_OR_NOT,
                    stakeType = TokenType.YRAL,
                    stakeAmount = HOT_OR_NOT_STAKE,
                    optionChosen = optionChosen,
                    gameResult = if (coinDelta > 0) GameResult.WIN else GameResult.LOSS,
                    wonLossAmount = coinDelta,
                    isTutorialVote = false,
                    affiliate = affiliateAttributionStore.peek(),
                ),
        )
    }

    companion object {
        private const val HOT_OR_NOT_STAKE = 1
    }
}
