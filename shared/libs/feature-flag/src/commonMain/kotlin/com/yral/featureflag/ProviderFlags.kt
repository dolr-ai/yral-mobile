package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup

/**
 * Catalog of provider-control flags. These are evaluated from Local provider only.
 */
object ProviderFlags : FlagGroup(keyPrefix = "provider", defaultAudience = FlagAudience.DEVELOPER) {
    val FirebaseEnabled: FeatureFlag<Boolean> =
        boolean(
            keySuffix = "firebase_remote_config_enabled",
            name = "Enable Firebase Provider",
            description = "Controls participation of Firebase Remote Config provider.",
            defaultValue = true,
        )
}
