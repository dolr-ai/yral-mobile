package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplashScreenViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SPLASH_SCREEN_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
) : BaseEventData(),
    EventData {
    public constructor() : this(
        FeatureEvents.SPLASH_SCREEN_VIEWED.getEventName(),
        Features.FEED.getFeatureName(),
    )
}

// --- Auth / Signup / Login ---
@Serializable
data class AuthScreenViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.AUTH_SCREEN_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("page_name") val pageName: SignupPageName,
) : BaseEventData(),
    EventData {
    public constructor(pageName: SignupPageName) : this(
        FeatureEvents.AUTH_SCREEN_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
        pageName,
    )
}

@Serializable
data class SignupClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("page_name") val pageName: SignupPageName,
) : BaseEventData(),
    EventData {
    constructor(pageName: SignupPageName) : this(
        FeatureEvents.SIGNUP_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        pageName,
    )
}

@Serializable
data class SignupJourneySelected(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_JOURNEY_SELECTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : BaseEventData(),
    EventData {
    constructor(authJourney: AuthJourney) : this(
        FeatureEvents.SIGNUP_JOURNEY_SELECTED.getEventName(),
        Features.AUTH.getFeatureName(),
        authJourney,
    )
}

@Serializable
data class SignupInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : BaseEventData(),
    EventData {
    constructor(authJourney: AuthJourney) : this(
        FeatureEvents.SIGNUP_INITIATED.getEventName(),
        Features.AUTH.getFeatureName(),
        authJourney,
    )
}

@Serializable
data class SignupSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("is_referral") val isReferral: Boolean,
    @SerialName("referrer_user_id") val referralUserID: String,
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : BaseEventData(),
    EventData {
    constructor(isReferral: Boolean, referralUserID: String, authJourney: AuthJourney) : this(
        FeatureEvents.SIGNUP_SUCCESS.getEventName(),
        Features.AUTH.getFeatureName(),
        isReferral,
        referralUserID,
        authJourney,
    )
}

@Serializable
data class LoginSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.LOGIN_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : BaseEventData(),
    EventData {
    constructor(authJourney: AuthJourney) : this(
        FeatureEvents.LOGIN_SUCCESS.getEventName(),
        Features.AUTH.getFeatureName(),
        authJourney,
    )
}

@Serializable
data class AuthFailedEventData(
    @SerialName("event") override val event: String = FeatureEvents.AUTH_FAILED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : BaseEventData(),
    EventData {
    constructor(authJourney: AuthJourney) : this(
        FeatureEvents.AUTH_FAILED.getEventName(),
        Features.AUTH.getFeatureName(),
        authJourney,
    )
}

// --- Home ---
@Serializable
data class HomePageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.HOME_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.HOME_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
    )
}

@Serializable
data class BottomNavigationClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.BOTTOM_NAVIGATION_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("category_name") val categoryName: CategoryName,
) : BaseEventData(),
    EventData {
    constructor(categoryName: CategoryName) : this(
        FeatureEvents.BOTTOM_NAVIGATION_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        categoryName,
    )
}

// --- Video ---

@Serializable
data class VideoStartedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_STARTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("is_game_enabled") val isGameEnabled: Boolean,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        likeCount: Long,
        shareCount: Long,
        viewCount: Long,
        isGameEnabled: Boolean,
        gameType: GameType,
        isNsfw: Boolean,
    ) : this(
        FeatureEvents.VIDEO_STARTED.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        isGameEnabled,
        gameType,
        isNsfw,
    )
}

@Serializable
data class VideoImpressionEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_IMPRESSION.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("category_name") val categoryName: CategoryName,
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("is_game_enabled") val isGameEnabled: Boolean,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        categoryName: CategoryName,
        videoId: String,
        publisherUserId: String,
        likeCount: Long,
        shareCount: Long,
        viewCount: Long,
        isGameEnabled: Boolean,
        gameType: GameType,
        isNsfw: Boolean,
    ) : this(
        FeatureEvents.VIDEO_IMPRESSION.getEventName(),
        Features.FEED.getFeatureName(),
        categoryName,
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        isGameEnabled,
        gameType,
        isNsfw,
    )
}

@Serializable
data class VideoViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("is_game_enabled") val isGameEnabled: Boolean,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        likeCount: Long,
        shareCount: Long,
        viewCount: Long,
        isGameEnabled: Boolean,
        gameType: GameType,
        isNsfw: Boolean,
    ) : this(
        FeatureEvents.VIDEO_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        isGameEnabled,
        gameType,
        isNsfw,
    )
}

@Serializable
data class VideoClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("is_game_enabled") val isGameEnabled: Boolean,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("cta_type") val ctaType: CtaType,
    @SerialName("page_name") val pageName: CategoryName,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        likeCount: Long,
        shareCount: Long,
        viewCount: Long,
        isGameEnabled: Boolean,
        gameType: GameType,
        isNsfw: Boolean,
        ctaType: CtaType,
        pageName: CategoryName,
    ) : this(
        FeatureEvents.VIDEO_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        isGameEnabled,
        gameType,
        isNsfw,
        ctaType,
        pageName,
    )
}

@Serializable
data class NsfwEnabledEventData(
    @SerialName("event") override val event: String = FeatureEvents.NSFW_ENABLED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("page_name") val pageName: String,
) : BaseEventData(),
    EventData {
    constructor(pageName: String) : this(
        FeatureEvents.NSFW_ENABLED.getEventName(),
        Features.AUTH.getFeatureName(),
        pageName,
    )
}

@Serializable
data class VideoReportedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_REPORTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("is_game_enabled") val isGameEnabled: Boolean,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("report_reason") val reason: String,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        isGameEnabled: Boolean,
        gameType: GameType,
        isNsfw: Boolean,
        reason: String,
    ) : this(
        FeatureEvents.VIDEO_REPORTED.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        isGameEnabled,
        gameType,
        isNsfw,
        reason,
    )
}

@Serializable
data class DeleteVideoInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.DELETE_VIDEO_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("page_name") val pageName: CategoryName,
    @SerialName("video_id") val videoId: String,
) : BaseEventData(),
    EventData {
    constructor(pageName: CategoryName, videoId: String) : this(
        FeatureEvents.DELETE_VIDEO_INITIATED.getEventName(),
        Features.AUTH.getFeatureName(),
        pageName,
        videoId,
    )
}

@Serializable
data class VideoDeletedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_DELETED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("page_name") val pageName: CategoryName,
    @SerialName("video_id") val videoId: String,
    @SerialName(value = "cta_type") val ctaType: VideoDeleteCTA,
) : BaseEventData(),
    EventData {
    constructor(pageName: CategoryName, videoId: String, ctaType: VideoDeleteCTA) : this(
        FeatureEvents.VIDEO_DELETED.getEventName(),
        Features.AUTH.getFeatureName(),
        pageName,
        videoId,
        ctaType,
    )
}

// --- Game ---
@Serializable
data class GameVotedEventData(
    @SerialName("event") override val event: String = FeatureEvents.GAME_VOTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("stake_amount") val stakeAmount: Int,
    @SerialName("stake_type") val stakeType: StakeType,
    @SerialName("option_chosen") val optionChosen: String,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        likeCount: Long,
        shareCount: Long,
        viewCount: Long,
        gameType: GameType,
        isNsfw: Boolean,
        stakeAmount: Int,
        stakeType: StakeType,
        optionChosen: String,
    ) : this(
        FeatureEvents.GAME_VOTED.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        gameType,
        isNsfw,
        stakeAmount,
        stakeType,
        optionChosen,
    )
}

@Serializable
data class GamePlayedEventData(
    @SerialName("event") override val event: String = FeatureEvents.GAME_PLAYED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("stake_amount") val stakeAmount: Int,
    @SerialName("stake_type") val stakeType: StakeType,
    @SerialName("option_chosen") val optionChosen: String,
    @SerialName("conclusion") val gameResult: GameResult,
    @SerialName("won_loss_amount") val wonLossAmount: Int,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        likeCount: Long,
        shareCount: Long,
        viewCount: Long,
        gameType: GameType,
        isNsfw: Boolean,
        stakeAmount: Int,
        stakeType: StakeType,
        optionChosen: String,
        gameResult: GameResult,
        wonLossAmount: Int,
    ) : this(
        FeatureEvents.GAME_PLAYED.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        gameType,
        isNsfw,
        stakeAmount,
        stakeType,
        optionChosen,
        gameResult,
        wonLossAmount,
    )
}

@Serializable
data class GameConcludedBottomsheetClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.GAME_CONCLUDED_BOTTOMSHEET_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("stake_amount") val stakeAmount: Int,
    @SerialName("stake_type") val stakeType: StakeType,
    @SerialName("conclusion") val gameResult: GameResult,
    @SerialName("won_loss_amount") val wonLossAmount: Int,
    @SerialName("cta_type") val ctaType: GameConcludedCtaType,
) : BaseEventData(),
    EventData {
    constructor(
        stakeAmount: Int,
        stakeType: StakeType,
        gameResult: GameResult,
        wonLossAmount: Int,
        ctaType: GameConcludedCtaType,
    ) : this(
        FeatureEvents.GAME_CONCLUDED_BOTTOMSHEET_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        stakeAmount,
        stakeType,
        gameResult,
        wonLossAmount,
        ctaType,
    )
}

// --- Menu ---
@Serializable
data class MenuPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.MENU_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.MENU.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.MENU_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
    )
}

@Serializable
data class MenuClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.MENU_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.MENU.getFeatureName(),
    @SerialName("cta_type") val ctaType: MenuCtaType,
) : BaseEventData(),
    EventData {
    constructor(ctaType: MenuCtaType) : this(
        FeatureEvents.MENU_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        ctaType,
    )
}

// --- Upload Video ---
@Serializable
data class UploadVideoPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.UPLOAD_VIDEO_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.UPLOAD_VIDEO_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
    )
}

@Serializable
data class SelectFileClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SELECT_FILE_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.SELECT_FILE_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
    )
}

@Serializable
data class FileSelectionSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.FILE_SELECTION_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("file_type") val fileType: String,
) : BaseEventData(),
    EventData {
    constructor(fileType: String) : this(
        FeatureEvents.FILE_SELECTION_SUCCESS.getEventName(),
        Features.AUTH.getFeatureName(),
        fileType,
    )
}

@Serializable
data class VideoUploadInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("caption_added") val captionAdded: Boolean,
    @SerialName("hashtags_added") val hashtagsAdded: Boolean,
) : BaseEventData(),
    EventData {
    constructor(captionAdded: Boolean, hashtagsAdded: Boolean) : this(
        FeatureEvents.VIDEO_UPLOAD_INITIATED.getEventName(),
        Features.AUTH.getFeatureName(),
        captionAdded,
        hashtagsAdded,
    )
}

@Serializable
data class VideoUploadSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("is_game_enabled") val isGameEnabled: Boolean,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        isGameEnabled: Boolean,
        gameType: GameType,
        isNsfw: Boolean,
    ) : this(
        FeatureEvents.VIDEO_UPLOAD_SUCCESS.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        isGameEnabled,
        gameType,
        isNsfw,
    )
}

@Serializable
data class VideoUploadErrorShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_ERROR_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("reason") val reason: String,
) : BaseEventData(),
    EventData {
    constructor(reason: String) : this(
        FeatureEvents.VIDEO_UPLOAD_ERROR_SHOWN.getEventName(),
        Features.AUTH.getFeatureName(),
        reason,
    )
}

// --- Profile ---
@Serializable
data class ProfilePageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.PROFILE_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
    @SerialName("total_videos") val totalVideos: Int,
    @SerialName("is_own_profile") val isOwnProfile: Boolean,
    @SerialName("publisher_user_id") val publisherUserId: String,
) : BaseEventData(),
    EventData {
    constructor(totalVideos: Int, isOwnProfile: Boolean, publisherUserId: String) : this(
        FeatureEvents.PROFILE_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
        totalVideos,
        isOwnProfile,
        publisherUserId,
    )
}

@Serializable
data class UploadVideoClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.UPLOAD_VIDEO_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
    @SerialName("page_name") val pageName: String = "profile",
) : BaseEventData(),
    EventData {
    constructor(pageName: String) : this(
        FeatureEvents.UPLOAD_VIDEO_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        pageName,
    )
}

// --- Wallet ---
@Serializable
data class CentsToDolrConvertedEventData(
    @SerialName("event") override val event: String = FeatureEvents.CENTS_TO_DOLR_CONVERTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.WALLET.getFeatureName(),
    @SerialName("cents_converted") val centsConverted: Double,
) : EventData

@Serializable
data class ThirdPartyWalletTransferredEventData(
    @SerialName("event") override val event: String = FeatureEvents.THIRD_PARTY_WALLET_TRANSFERRED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.WALLET.getFeatureName(),
    @SerialName("token_transferred") val tokenTransferred: Double,
) : EventData

@Serializable
data class SatsToBtcConvertedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SATS_TO_BTC_CONVERTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.WALLET.getFeatureName(),
    @SerialName("sats_converted") val satsConverted: Double,
) : EventData

// --- Refer & Earn ---
@Serializable
data class ReferAndEarnPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.REFER_AND_EARN_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.REFERRAL.getFeatureName(),
    @SerialName("earned_amount") val earnedAmount: Double,
) : EventData

@Serializable
data class ReferralHistoryViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.REFERRAL_HISTORY_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.REFERRAL.getFeatureName(),
    @SerialName("earned_amount") val earnedAmount: Double,
) : EventData

@Serializable
data class ShareInvitesClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SHARE_INVITES_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.REFERRAL.getFeatureName(),
    @SerialName("referral_bonus") val referralBonus: Double,
) : EventData

@Serializable
data class VideoDurationWatchedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_DURATION_WATCHED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("canister_id") val canisterId: String,
    @SerialName("creator_category") val creatorCategory: String = "",
    @SerialName("display_name") val displayName: String,
    @SerialName("feed_type") val feedType: String = "Clean",
    @SerialName("hashtag_count") val hashtagCount: Int,
    @SerialName("is_hot_or_not") val isHotOrNot: Boolean,
    @SerialName("is_logged_in") val isLoggedIn: Boolean,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("post_id") val postID: Long,
    @SerialName("publisher_canister_id") val publisherCanisterId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("share_count") val shareCount: Long = 0,
    @SerialName("user_id") val userID: String,
    @SerialName("video_category") val videoCategory: String = "",
    @SerialName("video_id") val videoID: String,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("nsfw_probability") val nsfwProbability: Double? = null,
    @SerialName("absolute_watched") val absoluteWatched: Double = 0.0,
    @SerialName("percentage_watched") val percentageWatched: Double = 0.0,
    @SerialName("video_duration") val videoDuration: Double = 0.0,
) : EventData

@Serializable
data class LeaderBoardPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.LEADERBOARD_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.LEADERBOARD.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.LEADERBOARD_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
    )
}

// Helper enums
@Serializable
enum class AuthJourney {
    @SerialName("google")
    GOOGLE,

    @SerialName("apple")
    APPLE,
}

@Serializable
enum class SignupPageName {
    @SerialName("feed")
    FEED,

    @SerialName("menu")
    MENU,
}

@Serializable
enum class CategoryName {
    @SerialName("upload_video")
    UPLOAD_VIDEO,

    @SerialName("leaderboard")
    LEADERBOARD,

    @SerialName("profile")
    PROFILE,

    @SerialName("menu")
    MENU,

    @SerialName("home")
    HOME,

    @SerialName("refer_and_earn")
    REFER_AND_EARN,

    @SerialName("wallet")
    WALLET,
}

@Serializable
enum class GameType {
    @SerialName("hot_or_not")
    HOT_OR_NOT,

    @SerialName("smiley")
    SMILEY,
}

@Serializable
enum class CtaType {
    @SerialName("like")
    LIKE,

    @SerialName("share")
    SHARE,

    @SerialName("refer_and_earn")
    REFER_AND_EARN,

    @SerialName("report")
    REPORT,

    @SerialName("creator_profile")
    CREATOR_PROFILE,

    @SerialName("nsfw_toggle")
    NSFW_TOGGLE,

    @SerialName("mute")
    MUTE,

    @SerialName("unmute")
    UNMUTE,

    @SerialName("delete")
    DELETE,

    @SerialName("video_play")
    PLAY,
}

@Serializable
enum class StakeType {
    @SerialName("cents")
    CENTS,

    @SerialName("sats")
    SATS,
}

@Serializable
enum class GameResult {
    @SerialName("win")
    WIN,

    @SerialName("loss")
    LOSS,
}

@Serializable
enum class NudgeType {
    @SerialName("bottomsheet")
    BOTTOMSHEET,

    @SerialName("banner")
    BANNER,
}

@Serializable
enum class GameConcludedCtaType {
    @SerialName("keep_playing")
    KEEP_PLAYING,

    @SerialName("learn_more")
    LEARN_MORE,
}

@Serializable
enum class MenuCtaType {
    @SerialName("login")
    LOGIN,

    @SerialName("talk_to_the_team")
    TALK_TO_THE_TEAM,

    @SerialName("terms_of_service")
    TERMS_OF_SERVICE,

    @SerialName("privacy_policy")
    PRIVACY_POLICY,

    @SerialName("log_out")
    LOG_OUT,

    @SerialName("delete_account")
    DELETE_ACCOUNT,

    @SerialName("follow_on")
    FOLLOW_ON,
}

@Serializable
enum class VideoDeleteCTA {
    @SerialName("profile_thumbnail")
    PROFILE_THUMBNAIL,

    @SerialName("video_fullscreen")
    VIDEO_FULLSCREEN,
}
