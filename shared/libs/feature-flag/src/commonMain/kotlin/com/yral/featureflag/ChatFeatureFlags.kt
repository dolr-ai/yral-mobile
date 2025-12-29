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
                defaultValue = false,
            )
    }
}
