package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AppFeatureFlags {
    object Common :
        FlagGroup(keyPrefix = "app", defaultAudience = FlagAudience.INTERNAL_QA) {
        val EnableSubscription: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "enableSubscription",
                name = "Enable Subscription",
                description = "Toggle subscription usage on App.",
                defaultValue = true,
            )
        val InitialBalanceReward: FeatureFlag<Int> =
            int(
                keySuffix = "initialBalanceReward",
                name = "Initial Balance Reward",
                description = "Initial balance reward",
                defaultValue = 25,
            )
        val MandatoryLogin: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "mandatoryLogin",
                name = "Mandatory Login",
                description = "Require users to sign in before accessing the app",
                defaultValue = false,
            )
        val BottomNavSwapEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "bottomNavSwapEnabled",
                name = "Bottom-nav swap (Discover first, Video feed second)",
                description = "When ON, swaps bottom-nav positions 1 and 2 so the chat " +
                    "Discover / Inbox tab (the one carrying the search bar from PR #1197) " +
                    "becomes the default landing — position 1 keeps the home-icon glyph but " +
                    "now opens Discover. The video feed moves to position 2 with a Reels-" +
                    "style play icon. Other nav slots (Upload, Wallet, Profile, Account) are " +
                    "untouched. When OFF, the nav order and default landing are exactly as " +
                    "today (Feed in position 1). Alpha-team-first rollout per 21γ.P16; instant " +
                    "rollback is a server-side flag flip. PR keeps defaultValue = false until " +
                    "alpha sign-off + ramp.",
                defaultValue = false,
            )
    }

    object Android :
        FlagGroup(keyPrefix = "app_android", defaultAudience = FlagAudience.INTERNAL_QA) {
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
