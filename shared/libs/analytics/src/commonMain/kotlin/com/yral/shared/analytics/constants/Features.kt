package com.yral.shared.analytics.constants

enum class Features {
    APP,
    AUTH,
    FEED,
    UPLOAD,
    PROFILE,
    MENU,
    WALLET,
    REFERRAL,
    LEADERBOARD,
    AI_CHATBOT,
    TOURNAMENT,
    SUBSCRIPTION,
    ;

    fun getFeatureName(): String = name.lowercase()
}
