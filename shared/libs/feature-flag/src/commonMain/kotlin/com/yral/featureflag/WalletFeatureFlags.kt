package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup

object WalletFeatureFlags {
    object Wallet :
        FlagGroup(keyPrefix = "wallet", defaultAudience = FlagAudience.INTERNAL_QA) {
        val Enabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "enabled",
                name = "Enable or disable wallet",
                description = "Enable or disable wallet",
                defaultValue = false,
            )
        val BtcRewardsLink: FeatureFlag<String> =
            string(
                keySuffix = "btcRewardsLink",
                name = "BTC Rewards Link",
                description = "BTC Rewards Link",
                defaultValue = "https://link.yral.com/dJqgFEnM6Wb",
            )
    }
}
