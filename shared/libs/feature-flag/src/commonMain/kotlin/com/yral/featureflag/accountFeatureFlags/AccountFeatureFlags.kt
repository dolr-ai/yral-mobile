package com.yral.featureflag.accountFeatureFlags

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AccountFeatureFlags {
    object AccountLinks :
        FlagGroup(keyPrefix = "account", defaultAudience = FlagAudience.INTERNAL_QA) {
        val Links: FeatureFlag<AccountLinksDto> =
            json(
                keySuffix = "links",
                name = "Account links",
                description = "Account links",
                defaultValue =
                    AccountLinksDto(
                        support = AccountDefaultLinks.TALK_TO_TEAM_URL,
                        tnc = AccountDefaultLinks.TERMS_OF_SERVICE_URL,
                        privacyPolicy = AccountDefaultLinks.PRIVACY_POLICY_URL,
                        telegram = AccountDefaultLinks.TELEGRAM_LINK,
                        discord = AccountDefaultLinks.DISCORD_LINK,
                        twitter = AccountDefaultLinks.TWITTER_LINK,
                    ),
                serializer = AccountLinksDto.serializer(),
            )
    }
}

@Serializable
data class AccountLinksDto(
    @SerialName("support") val support: String,
    @SerialName("support_text") val supportText: String? = null,
    @SerialName("support_icon") val supportIcon: String? = null,
    @SerialName("tnc") val tnc: String,
    @SerialName("privacy_policy") val privacyPolicy: String,
    @SerialName("telegram") val telegram: String,
    @SerialName("discord") val discord: String,
    @SerialName("twitter") val twitter: String,
)
