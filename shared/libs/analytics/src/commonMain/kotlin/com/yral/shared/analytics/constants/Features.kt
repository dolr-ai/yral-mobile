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
    ;

    fun getFeatureName(): String = name.lowercase()
}
