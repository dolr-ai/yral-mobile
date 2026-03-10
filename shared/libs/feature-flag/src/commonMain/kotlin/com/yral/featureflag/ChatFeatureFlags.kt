package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup

object ChatFeatureFlags {
    object Chat :
        FlagGroup(keyPrefix = "chat", defaultAudience = FlagAudience.INTERNAL_QA) {
        val Enabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "enabled",
                name = "Enable or disable chat",
                description = "Enable or disable chat",
                defaultValue = true,
            )
        val LoginPromptMessageThreshold: FeatureFlag<Int> =
            int(
                keySuffix = "loginPromptMessageThreshold",
                name = "Login prompt message limit",
                description = "Total message count at which to show login prompt in chat",
                defaultValue = 10,
            )
        val SubscriptionMandatoryThreshold: FeatureFlag<Int> =
            int(
                keySuffix = "subscriptionMandatoryThreshold",
                name = "Subscription mandatory threshold",
                description = "Total message count at which to show subscription nudge in chat",
                defaultValue = 1,
            )
        val MaxBotCountForCta: FeatureFlag<Int> =
            int(
                keySuffix = "maxBotCountForCta",
                name = "Max bot count for CTA",
                description = "Maximum number of bots before hiding the create influencer CTA",
                defaultValue = 3,
            )
        val MaxVisibleBotUsernames: FeatureFlag<Int> =
            int(
                keySuffix = "maxVisibleBotUsernames",
                name = "Max visible bot usernames",
                description = "Maximum number of bot usernames to show before '+N More'",
                defaultValue = 2,
            )
    }
}
