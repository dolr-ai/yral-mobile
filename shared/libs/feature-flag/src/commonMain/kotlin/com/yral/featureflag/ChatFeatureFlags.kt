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
                defaultValue = 70,
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
        val ChatAsHumanCreatorEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "chatAsHumanCreatorEnabled",
                name = "Chat as Human Creator (takeover)",
                description = "When ON, exposes the Take Over toggle inside a creator's inbox conversation view " +
                    "and renders user-side join/leave system banners. When OFF, the creator sees the legacy " +
                    "'switch to your human profile' prompt and system banners are not rendered.",
                defaultValue = false,
            )
    }
}
