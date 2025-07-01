package com.yral.shared.analytics.constants

/**
 * All analytic event names used across the app.
 */
enum class FeatureEvents {
    SPLASH_SCREEN_VIEWED,

    // Auth / Signup / Login
    AUTH_SCREEN_VIEWED,
    SIGNUP_CLICKED,
    SIGNUP_INITIATED,
    SIGNUP_SUCCESS,
    LOGIN_SUCCESS,
    AUTH_SUCCESSFUL,

    // Home
    HOME_PAGE_VIEWED,
    BOTTOM_NAVIGATION_CLICKED,

    // Video / Feed
    VIDEO_STARTED,
    VIDEO_VIEWED,
    VIDEO_CLICKED,
    NSFW_ENABLED,
    VIDEO_REPORTED,
    DELETE_VIDEO_INITIATED,
    VIDEO_DELETED,
    VIDEO_DURATION_WATCHED,
    EMPTY_COLD_START_FEED,
    DUPLICATE_POSTS,

    // Game
    GAME_PLAYED,
    GAME_CONCLUDED,
    GAME_CONCLUDED_BOTTOMSHEET_CLICKED,

    // Menu
    MENU_PAGE_VIEWED,
    MENU_CLICKED,

    // Upload
    UPLOAD_VIDEO_PAGE_VIEWED,
    SELECT_FILE_CLICKED,
    FILE_SELECTION_SUCCESS,
    VIDEO_UPLOAD_INITIATED,
    VIDEO_UPLOAD_SUCCESS,
    VIDEO_UPLOAD_ERROR_SHOWN,

    // Profile
    PROFILE_PAGE_VIEWED,
    UPLOAD_VIDEO_CLICKED,

    // Wallet
    CENTS_TO_DOLR_CONVERTED,
    THIRD_PARTY_WALLET_TRANSFERRED,
    SATS_TO_BTC_CONVERTED,

    // Referral
    REFER_AND_EARN_PAGE_VIEWED,
    REFERRAL_HISTORY_VIEWED,
    SHARE_INVITES_CLICKED,
    ;

    fun getEventName(): String =
        when (this) {
            VIDEO_DURATION_WATCHED -> "VideoDurationWatched"
            else -> name.lowercase()
        }
}
