package com.yral.featureflag.accountFeatureFlags

internal expect object AccountDefaultLinks {
    val TALK_TO_TEAM_URL: String
    val TERMS_OF_SERVICE_URL: String
    val PRIVACY_POLICY_URL: String
    val TELEGRAM_LINK: String
    val DISCORD_LINK: String
    val TWITTER_LINK: String
}
