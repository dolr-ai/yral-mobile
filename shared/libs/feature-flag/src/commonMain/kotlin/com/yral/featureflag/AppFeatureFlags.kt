package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AppFeatureFlags {
    object Android :
        FlagGroup(keyPrefix = "app_android", defaultAudience = FlagAudience.INTERNAL_QA) {
        val EnableAppCheck: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "enableAppCheck",
                name = "Enable Firebase App Check",
                description = "Toggle Firebase App Check usage on Android.",
                defaultValue = false,
            )
    }
    object Ios :
        FlagGroup(keyPrefix = "app_ios", defaultAudience = FlagAudience.INTERNAL_QA) {
        val InAppUpdate: FeatureFlag<IAPConfig> =
            json(
                keySuffix = "inAppUpdate",
                name = "In app update",
                description = "Force users to update app or prompt them",
                defaultValue =
                    IAPConfig(
                        minSupportedVersion = "1.0.0",
                        recommendedVersion = "1.0.0",
                    ),
                serializer = IAPConfig.serializer(),
            )
    }
}

@Serializable
data class IAPConfig(
    @SerialName("min_supported_version") val minSupportedVersion: String,
    @SerialName("recommended_version") val recommendedVersion: String,
)
