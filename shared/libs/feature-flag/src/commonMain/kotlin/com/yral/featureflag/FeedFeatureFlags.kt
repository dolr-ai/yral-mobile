package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup

object FeedFeatureFlags {
    object SmileyGame :
        FlagGroup(keyPrefix = "feed_smileyGame", defaultAudience = FlagAudience.INTERNAL_QA) {
        val StopAndVoteNudge: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "stopAndVoteNudge",
                name = "Enable Stop And Vote Nudge",
                description = "Block user to scroll to next video until game is played",
                defaultValue = false,
            )
        val AutoScrollEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "autoScrollEnabled",
                name = "Enable auto scroll after game vote",
                description = "Scrolls to next page after game is played",
                defaultValue = false,
            )
    }
    object GameMode :
        FlagGroup(keyPrefix = "feed_gameMode", defaultAudience = FlagAudience.INTERNAL_QA) {
        val Mode: FeatureFlag<String> =
            string(
                keySuffix = "mode",
                name = "Feed Game Mode",
                description = "Which game to show in feed: SMILEY or HOT_OR_NOT",
                defaultValue = "SMILEY",
            )
    }
    object FeedTypes :
        FlagGroup(keyPrefix = "feed", defaultAudience = FlagAudience.INTERNAL_QA) {
        val AvailableTypes: FeatureFlag<String> =
            string(
                keySuffix = "availableTypes",
                name = "Feed Type",
                description = "Available feed types for users",
                defaultValue = "ai",
            )
    }
    object CardLayout :
        FlagGroup(keyPrefix = "feed_cardLayout", defaultAudience = FlagAudience.INTERNAL_QA) {
        val Enabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "enabled",
                name = "Enable Card Layout",
                description = "Use Tinder-style swipeable card stack instead of vertical scroll",
                defaultValue = true,
            )
    }
}
