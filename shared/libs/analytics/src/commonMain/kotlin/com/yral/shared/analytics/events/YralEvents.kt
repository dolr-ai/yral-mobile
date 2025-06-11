package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Auth / Signup / Login ---
@Serializable
data class AuthScreenViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.AUTH_SCREEN_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
) : EventData

@Serializable
data class SignupClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : EventData

@Serializable
data class SignupInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : EventData

@Serializable
data class SignupSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("is_referral") val isReferral: Boolean,
    @SerialName("referrer_user_id") val referralUserID: String,
) : EventData

@Serializable
data class LoginSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.LOGIN_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
) : EventData

// --- Home ---
@Serializable
data class HomePageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.HOME_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
) : EventData

@Serializable
data class BottomNavigationClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.BOTTOM_NAVIGATION_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("category_name") val categoryName: CategoryName,
) : EventData

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
) : EventData

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
) : EventData

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
) : EventData

@Serializable
data class NsfwEnabledEventData(
    @SerialName("event") override val event: String = FeatureEvents.NSFW_ENABLED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("page_name") val pageName: String,
) : EventData

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
) : EventData

@Serializable
data class DeleteVideoInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.DELETE_VIDEO_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("page_name") val pageName: String = "profile",
    @SerialName("video_id") val videoId: String,
) : EventData

@Serializable
data class VideoDeletedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_DELETED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("page_name") val pageName: String = "profile",
    @SerialName("video_id") val videoId: String,
) : EventData

// --- Game ---
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
    @SerialName("token_type") val tokenType: TokenType,
    @SerialName("option_chosen") val optionChosen: GameOption,
    @SerialName("conclusion") val gameResult: GameResult,
    @SerialName("nudge_type") val nudgeType: NudgeType,
    @SerialName("creator_commission") val creatorCommission: Int,
    @SerialName("won_amount") val wonAmount: Int,
) : EventData

@Serializable
data class GameConcludedEventData(
    @SerialName("event") override val event: String = FeatureEvents.GAME_CONCLUDED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("stake_amount") val stakeAmount: Int,
    @SerialName("token_type") val tokenType: TokenType,
    @SerialName("option_chosen") val optionChosen: GameOption,
    @SerialName("conclusion") val gameResult: GameResult,
    @SerialName("nudge_type") val nudgeType: NudgeType,
    @SerialName("won_amount") val wonAmount: Int,
) : EventData

@Serializable
data class GameConcludedBottomsheetClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.GAME_CONCLUDED_BOTTOMSHEET_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("stake_amount") val stakeAmount: Int,
    @SerialName("token_type") val tokenType: TokenType,
    @SerialName("conclusion") val gameResult: GameResult,
    @SerialName("won_amount") val wonAmount: Int,
    @SerialName("cta_type") val ctaType: GameConcludedCtaType,
) : EventData

// --- Menu ---
@Serializable
data class MenuPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.MENU_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.MENU.getFeatureName(),
) : EventData

@Serializable
data class MenuClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.MENU_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.MENU.getFeatureName(),
    @SerialName("cta_type") val ctaType: MenuCtaType,
) : EventData

// --- Upload Video ---
@Serializable
data class UploadVideoPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.UPLOAD_VIDEO_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
) : EventData

@Serializable
data class SelectFileClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SELECT_FILE_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
) : EventData

@Serializable
data class FileSelectionSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.FILE_SELECTION_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("file_type") val fileType: String,
) : EventData

@Serializable
data class VideoUploadInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("caption_added") val captionAdded: Boolean,
    @SerialName("hashtags_added") val hashtagsAdded: Boolean,
) : EventData

@Serializable
data class VideoUploadSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("is_game_enabled") val isGameEnabled: Boolean,
    @SerialName("game_type") val gameType: GameType,
    @SerialName("is_nsfw") val isNsfw: Boolean,
) : EventData

@Serializable
data class VideoUploadErrorShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_ERROR_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("reason") val reason: String,
) : EventData

// --- Profile ---
@Serializable
data class ProfilePageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.PROFILE_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
    @SerialName("total_videos") val totalVideos: Int,
) : EventData

@Serializable
data class UploadVideoClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.UPLOAD_VIDEO_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
    @SerialName("page_name") val pageName: String = "profile",
) : EventData

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

// Helper enums
@Serializable
enum class AuthJourney {
    @SerialName("google")
    GOOGLE,

    @SerialName("facebook")
    FACEBOOK,
}

@Serializable
enum class CategoryName {
    @SerialName("upload_video")
    UPLOAD_VIDEO,

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
}

@Serializable
enum class TokenType {
    @SerialName("cents")
    CENTS,

    @SerialName("sats")
    SATS,
}

@Serializable
enum class GameOption {
    @SerialName("hot")
    HOT,

    @SerialName("not")
    NOT,

    @SerialName("laugh")
    LAUGH,

    @SerialName("heart")
    HEART,

    @SerialName("fire")
    FIRE,

    @SerialName("rocket")
    ROCKET,

    @SerialName("surprise")
    SURPRISE,
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
