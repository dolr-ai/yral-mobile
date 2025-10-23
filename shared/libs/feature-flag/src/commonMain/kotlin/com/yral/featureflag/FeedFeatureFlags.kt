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
    }
    object FeedTypes :
        FlagGroup(keyPrefix = "feed", defaultAudience = FlagAudience.INTERNAL_QA) {
        val AvailableTypes: FeatureFlag<String> =
            string(
                keySuffix = "availableTypes",
                name = "Feed Type",
                description = "Available feed types for users",
                defaultValue = "default",
            )
    }
}
