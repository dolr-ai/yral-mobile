package com.yral.shared.analytics.constants

enum class Features {
    AUTH,
    FEED,
    UPLOAD,
    PROFILE,
    MENU,
    WALLET,
    REFERRAL,
    LEADERBOARD,
    ;

    fun getFeatureName(): String = name.lowercase()
}
