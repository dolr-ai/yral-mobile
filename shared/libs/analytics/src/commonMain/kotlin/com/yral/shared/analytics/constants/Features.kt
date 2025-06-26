package com.yral.shared.analytics.constants

enum class Features {
    AUTH,
    FEED,
    UPLOAD,
    PROFILE,
    MENU,
    WALLET,
    REFERRAL,
    ;

    fun getFeatureName(): String = name.lowercase()
}
