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
                description = "User message count at which to show login prompt in chat",
                defaultValue = 5,
            )
        val SubscriptionMandatoryThreshold: FeatureFlag<Int> =
            int(
                keySuffix = "subscriptionMandatoryThreshold",
                name = "Subscription mandatory threshold",
                description = "User message count at which to show subscription nudge in chat",
                defaultValue = 15,
            )
    }
}
