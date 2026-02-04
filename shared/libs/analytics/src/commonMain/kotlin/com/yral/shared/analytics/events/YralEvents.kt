package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class FirstAppLaunchEventData(
    @SerialName("event") override val event: String = FeatureEvents.FIRST_APP_LAUNCH.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.APP.getFeatureName(),
    @SerialName("date_time") val dateTime: String,
) : BaseEventData(),
    EventData {
    public constructor(date: String) : this(
        FeatureEvents.FIRST_APP_LAUNCH.getEventName(),
        Features.APP.getFeatureName(),
        date,
    )
}

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

@Serializable
data class AppOnboardingShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.APP_ONBOARDING_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("onboarding_step") val onboardingStep: AnalyticsOnboardingStep,
) : BaseEventData(),
    EventData {
    constructor(step: AnalyticsOnboardingStep) : this(
        FeatureEvents.APP_ONBOARDING_SHOWN.getEventName(),
        Features.FEED.getFeatureName(),
        step,
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
    @SerialName("affiliate") val affiliate: String? = null,
) : BaseEventData(),
    EventData {
    constructor(
        isReferral: Boolean,
        referralUserID: String,
        authJourney: AuthJourney,
        affiliate: String? = null,
    ) : this(
        FeatureEvents.SIGNUP_SUCCESS.getEventName(),
        Features.AUTH.getFeatureName(),
        isReferral,
        referralUserID,
        authJourney,
        affiliate,
    )
}

@Serializable
data class LoginSuccessEventData(
    @SerialName("event") override val event: String = FeatureEvents.LOGIN_SUCCESS.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
    @SerialName("affiliate") val affiliate: String? = null,
) : BaseEventData(),
    EventData {
    constructor(authJourney: AuthJourney, affiliate: String? = null) : this(
        FeatureEvents.LOGIN_SUCCESS.getEventName(),
        Features.AUTH.getFeatureName(),
        authJourney,
        affiliate,
    )
}

@Serializable
data class AuthFailedEventData(
    @SerialName("event") override val event: String = FeatureEvents.AUTH_FAILED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("auth_journey") val authJourney: AuthJourney,
    @SerialName("affiliate") val affiliate: String? = null,
) : BaseEventData(),
    EventData {
    constructor(authJourney: AuthJourney, affiliate: String? = null) : this(
        FeatureEvents.AUTH_FAILED.getEventName(),
        Features.AUTH.getFeatureName(),
        authJourney,
        affiliate,
    )
}

@Serializable
data class AnonymousAuthFailedEventData(
    @SerialName("event") override val event: String = FeatureEvents.ANONYMOUS_AUTH_FAILED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("affiliate") val affiliate: String? = null,
    @SerialName("reason") val reason: String? = null,
) : BaseEventData(),
    EventData {
    constructor(
        affiliate: String? = null,
        reason: String? = null,
    ) : this(
        FeatureEvents.ANONYMOUS_AUTH_FAILED.getEventName(),
        Features.AUTH.getFeatureName(),
        affiliate,
        reason,
    )
}

@Serializable
data class AuthSessionStateChangedEventData(
    @SerialName("event") override val event: String = FeatureEvents.AUTH_SESSION_STATE_CHANGED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("from_state") val fromState: AuthSessionState,
    @SerialName("to_state") val toState: AuthSessionState,
    @SerialName("initiator") val initiator: AuthSessionInitiator,
    @SerialName("cause") val cause: AuthSessionCause,
    @SerialName("flow") val flow: AuthSessionFlow? = null,
) : BaseEventData(),
    EventData {
    constructor(
        fromState: AuthSessionState,
        toState: AuthSessionState,
        initiator: AuthSessionInitiator,
        cause: AuthSessionCause,
        flow: AuthSessionFlow? = null,
    ) : this(
        FeatureEvents.AUTH_SESSION_STATE_CHANGED.getEventName(),
        Features.AUTH.getFeatureName(),
        fromState,
        toState,
        initiator,
        cause,
        flow,
    )
}

@Serializable
enum class AuthSessionState {
    @SerialName("authenticated")
    AUTHENTICATED,

    @SerialName("unauthenticated")
    UNAUTHENTICATED,
}

@Serializable
enum class AuthSessionInitiator {
    @SerialName("user")
    USER,

    @SerialName("system")
    SYSTEM,
}

@Serializable
enum class AuthSessionCause {
    @SerialName("refresh_token_missing")
    REFRESH_TOKEN_MISSING,

    @SerialName("refresh_token_expired_or_invalid")
    REFRESH_TOKEN_EXPIRED_OR_INVALID,

    @SerialName("refresh_access_token_failed")
    REFRESH_ACCESS_TOKEN_FAILED,
}

@Serializable
enum class AuthSessionFlow {
    @SerialName("token_validation")
    TOKEN_VALIDATION,

    @SerialName("token_refresh")
    TOKEN_REFRESH,
}

@Serializable
data class IdentityTransitionEventData(
    @SerialName("event") override val event: String = FeatureEvents.IDENTITY_TRANSITION.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("previous_distinct_id") val previousDistinctId: String,
    @SerialName("new_distinct_id") val newDistinctId: String,
    @SerialName("reset_reason") val resetReason: String,
) : BaseEventData(),
    EventData

@Serializable
data class SignupNudgeDismissedEventData(
    @SerialName("event") override val event: String = FeatureEvents.SIGNUP_NUDGE_DISMISSED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("dismiss_action") val dismissAction: SignupNudgeDismissAction,
) : BaseEventData(),
    EventData {
    constructor(dismissAction: SignupNudgeDismissAction) : this(
        FeatureEvents.SIGNUP_NUDGE_DISMISSED.getEventName(),
        Features.AUTH.getFeatureName(),
        dismissAction,
    )
}

@Serializable
enum class SignupNudgeDismissAction {
    @SerialName("close")
    CLOSE,

    @SerialName("skip")
    SKIP,
}

@Serializable
data class PhoneNumberEnteredEventData(
    @SerialName("event") override val event: String = FeatureEvents.PHONE_NUMBER_ENTERED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("country_code") val countryCode: String,
    @SerialName("phone_length") val phoneLength: Int,
) : BaseEventData(),
    EventData {
    constructor(
        countryCode: String,
        phoneLength: Int,
    ) : this(
        FeatureEvents.PHONE_NUMBER_ENTERED.getEventName(),
        Features.AUTH.getFeatureName(),
        countryCode,
        phoneLength,
    )
}

@Serializable
data class OtpRequestInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.OTP_REQUEST_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("attempt_number") val attemptNumber: Int,
    @SerialName("request_type") val requestType: OtpRequestType,
) : BaseEventData(),
    EventData {
    constructor(
        attemptNumber: Int,
        requestType: OtpRequestType,
    ) : this(
        FeatureEvents.OTP_REQUEST_INITIATED.getEventName(),
        Features.AUTH.getFeatureName(),
        attemptNumber,
        requestType,
    )
}

@Serializable
enum class OtpRequestType {
    @SerialName("initial")
    INITIAL,

    @SerialName("resend")
    RESEND,
}

@Serializable
data class OtpScreenViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.OTP_SCREEN_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("phone_number") val phoneNumber: String,
) : BaseEventData(),
    EventData {
    constructor(phoneNumber: String) : this(
        FeatureEvents.OTP_SCREEN_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
        phoneNumber,
    )
}

@Serializable
data class OtpValidationResultEventData(
    @SerialName("event") override val event: String = FeatureEvents.OTP_VALIDATION_RESULT.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
    @SerialName("validation_status") val validationStatus: OtpValidationStatus,
    @SerialName("failure_reason") val failureReason: String? = null,
) : BaseEventData(),
    EventData {
    constructor(
        validationStatus: OtpValidationStatus,
        failureReason: String? = null,
    ) : this(
        FeatureEvents.OTP_VALIDATION_RESULT.getEventName(),
        Features.AUTH.getFeatureName(),
        validationStatus,
        failureReason,
    )
}

@Serializable
enum class OtpValidationStatus {
    @SerialName("success")
    SUCCESS,

    @SerialName("failure")
    FAILURE,
}

// --- Home ---
@Serializable
data class HomePageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.HOME_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("affiliate") val affiliate: String? = null,
) : BaseEventData(),
    EventData {
    constructor(affiliate: String? = null) : this(
        FeatureEvents.HOME_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
        affiliate,
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

@Serializable
data class FeedToggleClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.FEED_TOGGLE_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("feed_type") val feedType: FeedType,
    @SerialName("is_expanded") val isExpanded: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        feedType: FeedType,
        isExpanded: Boolean,
    ) : this(
        FeatureEvents.FEED_TOGGLE_CLICKED.getEventName(),
        Features.FEED.getFeatureName(),
        feedType,
        isExpanded,
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
        FeatureEvents.VIDEO_IMPRESSION.getEventName(),
        Features.FEED.getFeatureName(),
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

@Serializable
data class VideoDownloadedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_DOWNLOADED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
    @SerialName("video_id") val videoId: String,
) : BaseEventData(),
    EventData {
    constructor(videoId: String) : this(
        FeatureEvents.VIDEO_DOWNLOADED.getEventName(),
        Features.PROFILE.getFeatureName(),
        videoId,
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
    @SerialName("stake_type") val stakeType: TokenType,
    @SerialName("option_chosen") val optionChosen: String,
    @SerialName("is_tutorial_vote") val isTutorialVote: Boolean,
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
        stakeType: TokenType,
        optionChosen: String,
        isTutorialVote: Boolean,
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
        isTutorialVote,
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
    @SerialName("stake_type") val stakeType: TokenType,
    @SerialName("option_chosen") val optionChosen: String,
    @SerialName("conclusion") val gameResult: GameResult,
    @SerialName("won_loss_amount") val wonLossAmount: Int,
    @SerialName("is_tutorial_vote") val isTutorialVote: Boolean,
    @SerialName("affiliate") val affiliate: String? = null,
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
        stakeType: TokenType,
        optionChosen: String,
        gameResult: GameResult,
        wonLossAmount: Int,
        isTutorialVote: Boolean,
        affiliate: String? = null,
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
        abs(wonLossAmount),
        isTutorialVote,
        affiliate,
    )
}

@Serializable
data class GameConcludedBottomsheetClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.GAME_CONCLUDED_BOTTOMSHEET_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("stake_amount") val stakeAmount: Int,
    @SerialName("stake_type") val stakeType: TokenType,
    @SerialName("conclusion") val gameResult: GameResult,
    @SerialName("won_loss_amount") val wonLossAmount: Int,
    @SerialName("cta_type") val ctaType: GameConcludedCtaType,
) : BaseEventData(),
    EventData {
    constructor(
        stakeAmount: Int,
        stakeType: TokenType,
        gameResult: GameResult,
        wonLossAmount: Int,
        ctaType: GameConcludedCtaType,
    ) : this(
        FeatureEvents.GAME_CONCLUDED_BOTTOMSHEET_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        stakeAmount,
        stakeType,
        gameResult,
        abs(wonLossAmount),
        ctaType,
    )
}

@Serializable
data class GameTutorialShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.GAME_TUTORIAL_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
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
        gameType: GameType,
        isNsfw: Boolean,
    ) : this(
        FeatureEvents.GAME_TUTORIAL_SHOWN.getEventName(),
        Features.FEED.getFeatureName(),
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        gameType,
        isNsfw,
    )
}

@Serializable
data class ForcedGameplayNudgeShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.FORCED_GAMEPLAY_NUDGE_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("share_count") val shareCount: Long,
    @SerialName("view_count") val viewCount: Long,
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
        gameType: GameType,
        isNsfw: Boolean,
    ) : this(
        FeatureEvents.FORCED_GAMEPLAY_NUDGE_SHOWN.getEventName(),
        Features.FEED.getFeatureName(),
        videoId,
        publisherUserId,
        likeCount,
        shareCount,
        viewCount,
        gameType,
        isNsfw,
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
data class VideoCreationPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.UPLOAD_VIDEO_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("type_ext") val type: VideoCreationType,
    @SerialName("credits_fetched") val creditsFetched: Boolean?,
    @SerialName("credits_available") val creditsAvailable: Int?,
) : BaseEventData(),
    EventData {
    constructor(
        type: VideoCreationType,
        creditsFetched: Boolean? = null,
        creditsAvailable: Int? = null,
    ) : this(
        FeatureEvents.VIDEO_CREATION_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
        type,
        creditsFetched,
        creditsAvailable,
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
    @SerialName("type_ext") val type: VideoCreationType,
    @SerialName("affiliate") val affiliate: String? = null,
) : BaseEventData(),
    EventData {
    constructor(
        captionAdded: Boolean,
        hashtagsAdded: Boolean,
        type: VideoCreationType,
        affiliate: String? = null,
    ) : this(
        FeatureEvents.VIDEO_UPLOAD_INITIATED.getEventName(),
        Features.AUTH.getFeatureName(),
        captionAdded,
        hashtagsAdded,
        type,
        affiliate,
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
    @SerialName("type_ext") val type: VideoCreationType,
    @SerialName("affiliate") val affiliate: String? = null,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        publisherUserId: String,
        isGameEnabled: Boolean,
        gameType: GameType,
        isNsfw: Boolean,
        type: VideoCreationType,
        affiliate: String? = null,
    ) : this(
        FeatureEvents.VIDEO_UPLOAD_SUCCESS.getEventName(),
        Features.AUTH.getFeatureName(),
        videoId,
        publisherUserId,
        isGameEnabled,
        gameType,
        isNsfw,
        type,
        affiliate,
    )
}

@Serializable
data class VideoUploadErrorShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_ERROR_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("reason") val reason: String,
    @SerialName("type_ext") val type: VideoCreationType,
    @SerialName("affiliate") val affiliate: String? = null,
) : BaseEventData(),
    EventData {
    constructor(
        reason: String,
        type: VideoCreationType,
        affiliate: String? = null,
    ) : this(
        FeatureEvents.VIDEO_UPLOAD_ERROR_SHOWN.getEventName(),
        Features.AUTH.getFeatureName(),
        reason,
        type,
        affiliate,
    )
}

@Serializable
data class VideoUploadTypeSelectedData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_ERROR_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("type_ext") val type: VideoCreationType,
) : BaseEventData(),
    EventData {
    constructor(type: VideoCreationType) : this(
        FeatureEvents.VIDEO_UPLOAD_TYPE_SELECTED.getEventName(),
        Features.AUTH.getFeatureName(),
        type,
    )
}

@Serializable
data class VideoGenerationModelSelectedData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_UPLOAD_ERROR_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("model") val model: String,
) : BaseEventData(),
    EventData {
    constructor(model: String) : this(
        FeatureEvents.VIDEO_GENERATION_MODEL_SELECTED.getEventName(),
        Features.AUTH.getFeatureName(),
        model,
    )
}

@Serializable
data class CreateAIVideoClickedData(
    @SerialName("event") override val event: String = FeatureEvents.CREATE_AI_VIDEO_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("model") val model: String,
    @SerialName("prompt") val prompt: String,
) : BaseEventData(),
    EventData {
    constructor(model: String, prompt: String) : this(
        FeatureEvents.CREATE_AI_VIDEO_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        model,
        prompt,
    )
}

@Serializable
data class AiVideoGeneratedData(
    @SerialName("event") override val event: String = FeatureEvents.AI_VIDEO_GENERATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.UPLOAD.getFeatureName(),
    @SerialName("model") val model: String,
    @SerialName("prompt") val prompt: String,
    @SerialName("is_success") val isSuccess: Boolean,
    @SerialName("reason") val reason: String?,
    @SerialName("reason_type") val reasonType: AiVideoGenFailureType?,
) : BaseEventData(),
    EventData {
    constructor(
        model: String,
        prompt: String,
        isSuccess: Boolean,
        reason: String?,
        reasonType: AiVideoGenFailureType?,
    ) : this(
        FeatureEvents.AI_VIDEO_GENERATED.getEventName(),
        Features.AUTH.getFeatureName(),
        model,
        prompt,
        isSuccess,
        reason,
        reasonType,
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
data class EditProfileStartedEventData(
    @SerialName("event") override val event: String = FeatureEvents.EDIT_PROFILE_STARTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
    @SerialName("source") val source: EditProfileSource,
) : BaseEventData(),
    EventData {
    constructor(source: EditProfileSource) : this(
        FeatureEvents.EDIT_PROFILE_STARTED.getEventName(),
        Features.PROFILE.getFeatureName(),
        source,
    )
}

@Serializable
data class EditProfileCompletedEventData(
    @SerialName("event") override val event: String = FeatureEvents.EDIT_PROFILE_COMPLETED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
    @SerialName("username_updated") val usernameUpdated: Boolean,
    @SerialName("profile_image_updated") val profileImageUpdated: Boolean,
    @SerialName("bio_updated") val bioUpdated: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        usernameUpdated: Boolean,
        profileImageUpdated: Boolean,
        bioUpdated: Boolean,
    ) : this(
        FeatureEvents.EDIT_PROFILE_COMPLETED.getEventName(),
        Features.PROFILE.getFeatureName(),
        usernameUpdated,
        profileImageUpdated,
        bioUpdated,
    )
}

@Serializable
data class EditProfileCancelledEventData(
    @SerialName("event") override val event: String = FeatureEvents.EDIT_PROFILE_CANCELLED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.PROFILE.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.EDIT_PROFILE_CANCELLED.getEventName(),
        Features.PROFILE.getFeatureName(),
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

@Serializable
data class PushNotificationsPopupEventData(
    @SerialName("event") override val event: String = FeatureEvents.ENABLE_PUSH_NOTIFICATION_POPUP_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("source") val source: AnalyticsAlertsRequestType,
) : BaseEventData(),
    EventData {
    constructor(source: AnalyticsAlertsRequestType) : this(
        FeatureEvents.ENABLE_PUSH_NOTIFICATION_POPUP_SHOWN.getEventName(),
        Features.FEED.getFeatureName(),
        source,
    )
}

@Serializable
data class PushNotificationsEnabledEventData(
    @SerialName("event") override val event: String = FeatureEvents.NOTIFICATIONS_ENABLED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("source") val source: AnalyticsAlertsRequestType,
) : BaseEventData(),
    EventData {
    constructor(source: AnalyticsAlertsRequestType) : this(
        FeatureEvents.NOTIFICATIONS_ENABLED.getEventName(),
        Features.FEED.getFeatureName(),
        source,
    )
}

@Serializable
data class AirdropClaimedEventData(
    @SerialName("event") override val event: String = FeatureEvents.AIRDROP_CLAIMED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("token_type") val tokenType: TokenType,
    @SerialName("is_success") val isSuccess: Boolean,
    @SerialName("claimed_amount") val claimedAmount: Int,
    @SerialName("is_auto_credited") val isAutoCredited: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        tokenType: TokenType,
        isSuccess: Boolean,
        claimedAmount: Int,
        isAutoCredited: Boolean,
    ) : this(
        FeatureEvents.AIRDROP_CLAIMED.getEventName(),
        Features.FEED.getFeatureName(),
        tokenType,
        isSuccess,
        claimedAmount,
        isAutoCredited,
    )
}

@Serializable
data class UserFollowedEventData(
    @SerialName("event") override val event: String = FeatureEvents.USER_FOLLOWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("publisher_user_id")
    val publisherUserId: String,
    @SerialName("source")
    val source: SourceScreen,
    @SerialName("cta_type")
    val ctaType: CtaType,
) : BaseEventData(),
    EventData {
    constructor(
        publisherUserId: String,
        source: SourceScreen,
        ctaType: CtaType,
    ) : this(
        FeatureEvents.USER_FOLLOWED.getEventName(),
        Features.FEED.getFeatureName(),
        publisherUserId,
        source,
        ctaType,
    )
}

@Serializable
data class UserUnFollowedEventData(
    @SerialName("event") override val event: String = FeatureEvents.USER_UNFOLLOWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("publisher_user_id")
    val publisherUserId: String,
    @SerialName("source")
    val source: SourceScreen,
    @SerialName("cta_type")
    val ctaType: CtaType,
) : BaseEventData(),
    EventData {
    constructor(
        publisherUserId: String,
        source: SourceScreen,
        ctaType: CtaType,
    ) : this(
        FeatureEvents.USER_UNFOLLOWED.getEventName(),
        Features.FEED.getFeatureName(),
        publisherUserId,
        source,
        ctaType,
    )
}

@Serializable
data class FollowersListViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.FOLLOWERS_LIST_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("publisher_user_id")
    val publisherUserId: String,
    @SerialName("total_count")
    val totalCount: Long,
    @SerialName("tab")
    val tab: FollowersListTab,
) : BaseEventData(),
    EventData {
    constructor(
        publisherUserId: String,
        tab: FollowersListTab,
        totalCount: Long,
    ) : this(
        FeatureEvents.FOLLOWERS_LIST_VIEWED.getEventName(),
        Features.FEED.getFeatureName(),
        publisherUserId,
        totalCount,
        tab,
    )
}

// --- Wallet ---
@Serializable
data class WalletPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.WALLET_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.WALLET.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.WALLET_PAGE_VIEWED.getEventName(),
        Features.WALLET.getFeatureName(),
    )
}

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

@Serializable
data class VideoViewsRewardsNudgeShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_VIEWS_REWARDS_NUDGE_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.WALLET.getFeatureName(),
    @SerialName("video_id") val videoId: String?,
    @SerialName("current_views") val currentViews: Long?,
    @SerialName("reward_amount_btc") val rewardAmountBtc: Double?,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String?,
        currentViews: Long?,
        rewardAmountBtc: Double?,
    ) : this(
        FeatureEvents.VIDEO_VIEWS_REWARDS_NUDGE_SHOWN.getEventName(),
        Features.WALLET.getFeatureName(),
        videoId,
        currentViews,
        rewardAmountBtc,
    )
}

@Serializable
data class HowToEarnClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.HOW_TO_EARN_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.WALLET.getFeatureName(),
) : BaseEventData(),
    EventData {
    constructor() : this(
        FeatureEvents.HOW_TO_EARN_CLICKED.getEventName(),
        Features.WALLET.getFeatureName(),
    )
}

// --- Refer & Earn ---
@Serializable
data class ReferralReceivedEventData(
    @SerialName("event") override val event: String = FeatureEvents.REFERRAL_RECEIVED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.REFERRAL.getFeatureName(),
    @SerialName("source") val source: String?,
    @SerialName("medium") val medium: String?,
    @SerialName("campaign") val campaign: String?,
    @SerialName("term") val term: String?,
    @SerialName("content") val content: String?,
) : BaseEventData(),
    EventData {
    constructor(
        source: String?,
        medium: String?,
        campaign: String?,
        term: String?,
        content: String?,
    ) : this(
        FeatureEvents.REFERRAL_RECEIVED.getEventName(),
        Features.REFERRAL.getFeatureName(),
        source,
        medium,
        campaign,
        term,
        content,
    )
}

@Serializable
data class AttributionFailedEventData(
    @SerialName("event") override val event: String = FeatureEvents.ATTRIBUTION_FAILED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.REFERRAL.getFeatureName(),
    @SerialName("reason") val reason: String,
    @SerialName("error_code") val errorCode: Int? = null,
    @SerialName("raw_referrer") val rawReferrer: String? = null,
    @SerialName("processors_checked") val processorsChecked: List<String>? = null,
    @SerialName("is_enterprise_device") val isEnterpriseDevice: Boolean? = null,
    @SerialName("has_work_profile") val hasWorkProfile: Boolean? = null,
) : BaseEventData(),
    EventData {
    constructor(
        reason: String,
        errorCode: Int? = null,
        rawReferrer: String? = null,
        processorsChecked: List<String>? = null,
        isEnterpriseDevice: Boolean? = null,
        hasWorkProfile: Boolean? = null,
    ) : this(
        FeatureEvents.ATTRIBUTION_FAILED.getEventName(),
        Features.REFERRAL.getFeatureName(),
        reason,
        errorCode,
        rawReferrer,
        processorsChecked,
        isEnterpriseDevice,
        hasWorkProfile,
    )
}

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
    @SerialName("post_id") val postID: String,
    @SerialName("publisher_canister_id") val publisherCanisterId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("share_count") val shareCount: Long = 0,
    @SerialName("user_id") val userID: String,
    @SerialName("video_category") val videoCategory: String = "",
    @SerialName("video_id") val videoID: String,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("nsfw_probability") val nsfwProbability: Double,
    @SerialName("absolute_watched") val absoluteWatched: Double = 0.0,
    @SerialName("percentage_watched") val percentageWatched: Double = 0.0,
    @SerialName("video_duration") val videoDuration: Double = 0.0,
) : EventData

@Serializable
data class OtpDismissedEventData(
    @SerialName("event") override val event: String = FeatureEvents.OTP_DISMISSED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AUTH.getFeatureName(),
) : BaseEventData(),
    EventData

// --- Share ---
@Serializable
data class VideoShareClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_SHARE_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
    @SerialName("source_screen") val sourceScreen: SourceScreen,
    @SerialName("is_owner") val isOwner: Boolean,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
        sourceScreen: SourceScreen,
        isOwner: Boolean,
    ) : this(
        FeatureEvents.VIDEO_SHARE_CLICKED.getEventName(),
        Features.FEED.getFeatureName(),
        videoId,
        sourceScreen,
        isOwner,
    )
}

@Serializable
data class ShareAppOpenedFromLinkEventData(
    @SerialName("event") override val event: String = FeatureEvents.SHARE_APP_OPENED_FROM_LINK.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("video_id") val videoId: String,
) : BaseEventData(),
    EventData {
    constructor(
        videoId: String,
    ) : this(
        FeatureEvents.SHARE_APP_OPENED_FROM_LINK.getEventName(),
        Features.FEED.getFeatureName(),
        videoId,
    )
}

@Serializable
enum class SourceScreen {
    @SerialName("homefeed")
    HOMEFEED,

    @SerialName("profile")
    PROFILE,
}

@Serializable
data class InfluencerCardsViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.INFLUENCER_CARDS_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AI_CHATBOT.getFeatureName(),
    @SerialName("influencers_shown") val influencersShown: List<String>,
    @SerialName("total_cards") val totalCards: Int,
) : BaseEventData(),
    EventData {
    constructor(influencersShown: List<String>, totalCards: Int) : this(
        FeatureEvents.INFLUENCER_CARDS_VIEWED.getEventName(),
        Features.AI_CHATBOT.getFeatureName(),
        influencersShown,
        totalCards,
    )
}

@Serializable
data class InfluencerCardClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.INFLUENCER_CARD_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AI_CHATBOT.getFeatureName(),
    @SerialName("influencer_id") val influencerId: String,
    @SerialName("influencer_type") val influencerType: String,
    @SerialName("click_type") val clickType: InfluencerClickType,
    @SerialName("position") val position: Int,
) : BaseEventData(),
    EventData {
    constructor(
        influencerId: String,
        influencerType: String,
        clickType: InfluencerClickType,
        position: Int,
    ) : this(
        FeatureEvents.INFLUENCER_CARD_CLICKED.getEventName(),
        Features.AI_CHATBOT.getFeatureName(),
        influencerId,
        influencerType,
        clickType,
        position,
    )
}

@Serializable
data class ChatInfluencerClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.CHAT_INFLUENCER_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AI_CHATBOT.getFeatureName(),
    @SerialName("influencer_id") val influencerId: String,
    @SerialName("influencer_type") val influencerType: String,
    @SerialName("source") val source: InfluencerSource,
) : BaseEventData(),
    EventData {
    constructor(
        influencerId: String,
        influencerType: String,
        source: InfluencerSource,
    ) : this(
        FeatureEvents.CHAT_INFLUENCER_CLICKED.getEventName(),
        Features.AI_CHATBOT.getFeatureName(),
        influencerId,
        influencerType,
        source,
    )
}

@Serializable
data class ChatSessionStartedEventData(
    @SerialName("event") override val event: String = FeatureEvents.CHAT_SESSION_STARTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AI_CHATBOT.getFeatureName(),
    @SerialName("influencer_id") val influencerId: String,
    @SerialName("influencer_type") val influencerType: String,
    @SerialName("chat_session_id") val chatSessionId: String,
    @SerialName("source") val source: InfluencerSource,
) : BaseEventData(),
    EventData {
    constructor(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        source: InfluencerSource,
    ) : this(
        FeatureEvents.CHAT_SESSION_STARTED.getEventName(),
        Features.AI_CHATBOT.getFeatureName(),
        influencerId,
        influencerType,
        chatSessionId,
        source,
    )
}

@Serializable
data class UserMessageSentEventData(
    @SerialName("event") override val event: String = FeatureEvents.USER_MESSAGE_SENT.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AI_CHATBOT.getFeatureName(),
    @SerialName("influencer_id") val influencerId: String,
    @SerialName("influencer_type") val influencerType: String,
    @SerialName("chat_session_id") val chatSessionId: String,
    @SerialName("message_length") val messageLength: Int,
    @SerialName("message_type") val messageType: String,
    @SerialName("message") val message: String,
) : BaseEventData(),
    EventData {
    constructor(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        messageLength: Int,
        messageType: String,
        message: String,
    ) : this(
        FeatureEvents.USER_MESSAGE_SENT.getEventName(),
        Features.AI_CHATBOT.getFeatureName(),
        influencerId,
        influencerType,
        chatSessionId,
        messageLength,
        messageType,
        message,
    )
}

@Serializable
data class AIMessageDeliveredEventData(
    @SerialName("event") override val event: String = FeatureEvents.AI_MESSAGE_DELIVERED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.AI_CHATBOT.getFeatureName(),
    @SerialName("influencer_id") val influencerId: String,
    @SerialName("influencer_type") val influencerType: String,
    @SerialName("chat_session_id") val chatSessionId: String,
    @SerialName("response_latency_ms") val responseLatencyMs: Int,
    @SerialName("response_length") val responseLength: Int,
    @SerialName("message") val message: String,
) : BaseEventData(),
    EventData {
    constructor(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        responseLatencyMs: Int,
        responseLength: Int,
        message: String,
    ) : this(
        FeatureEvents.AI_MESSAGE_DELIVERED.getEventName(),
        Features.AI_CHATBOT.getFeatureName(),
        influencerId,
        influencerType,
        chatSessionId,
        responseLatencyMs,
        responseLength,
        message,
    )
}

@Serializable
data class LeaderBoardPageViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.LEADERBOARD_PAGE_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.LEADERBOARD.getFeatureName(),
    @SerialName("leaderboard_tab") val leaderBoardTabType: LeaderBoardTabType,
) : BaseEventData(),
    EventData {
    constructor(leaderBoardTabType: LeaderBoardTabType) : this(
        FeatureEvents.LEADERBOARD_PAGE_VIEWED.getEventName(),
        Features.AUTH.getFeatureName(),
        leaderBoardTabType,
    )
}

@Serializable
data class LeaderBoardPageLoadedEventData(
    @SerialName("event") override val event: String = FeatureEvents.LEADERBOARD_PAGE_LOADED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.LEADERBOARD.getFeatureName(),
    @SerialName("leaderboard_tab") val leaderBoardTabType: LeaderBoardTabType,
    @SerialName("rank") val rank: Int,
    @SerialName("visible_rows") val visibleRows: Int?,
) : BaseEventData(),
    EventData {
    constructor(leaderBoardTabType: LeaderBoardTabType, rank: Int, visibleRows: Int?) : this(
        FeatureEvents.LEADERBOARD_PAGE_LOADED.getEventName(),
        Features.AUTH.getFeatureName(),
        leaderBoardTabType,
        rank,
        visibleRows,
    )
}

@Serializable
data class LeaderBoardTabClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.LEADERBOARD_TAB_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.LEADERBOARD.getFeatureName(),
    @SerialName("leaderboard_tab") val leaderBoardTabType: LeaderBoardTabType,
) : BaseEventData(),
    EventData {
    constructor(leaderBoardTabType: LeaderBoardTabType) : this(
        FeatureEvents.LEADERBOARD_TAB_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        leaderBoardTabType,
    )
}

@Serializable
data class LeaderBoardCalendarClickedEventData(
    @SerialName("event") override val event: String = FeatureEvents.LEADERBOARD_CALENDAR_CLICKED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.LEADERBOARD.getFeatureName(),
    @SerialName("leaderboard_tab") val leaderBoardTabType: LeaderBoardTabType,
    @SerialName("rank") val rank: Int,
) : BaseEventData(),
    EventData {
    constructor(leaderBoardTabType: LeaderBoardTabType, rank: Int) : this(
        FeatureEvents.LEADERBOARD_CALENDAR_CLICKED.getEventName(),
        Features.AUTH.getFeatureName(),
        leaderBoardTabType,
        rank,
    )
}

@Serializable
data class LeaderBoardDaySelectedEventData(
    @SerialName("event") override val event: String = FeatureEvents.LEADERBOARD_DAY_SELECTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.LEADERBOARD.getFeatureName(),
    @SerialName("day") val day: Int,
    @SerialName("date") val string: String,
    @SerialName("rank") val rank: Int,
    @SerialName("visible_rows") val visibleRows: Int?,
) : BaseEventData(),
    EventData {
    constructor(day: Int, date: String, rank: Int, visibleRows: Int?) : this(
        FeatureEvents.LEADERBOARD_DAY_SELECTED.getEventName(),
        Features.AUTH.getFeatureName(),
        day,
        date,
        rank,
        visibleRows,
    )
}

// Helper enums
@Serializable
enum class InfluencerClickType {
    @SerialName("talk")
    TALK,

    @SerialName("view_profile")
    VIEW_PROFILE,
}

@Serializable
enum class InfluencerSource {
    @SerialName("card")
    CARD,

    @SerialName("profile")
    PROFILE,
}

@Serializable
enum class AuthJourney {
    @SerialName("google")
    GOOGLE,

    @SerialName("apple")
    APPLE,

    @SerialName("phone")
    PHONE,
}

@Serializable
enum class SignupPageName {
    @SerialName("splash")
    SPLASH,

    @SerialName("home")
    HOME,

    @SerialName("menu")
    MENU,

    @SerialName("conversation")
    CONVERSATION,

    @SerialName("profile")
    PROFILE,

    @SerialName("video_creation")
    VIDEO_CREATION,

    @SerialName("upload_video")
    UPLOAD_VIDEO,

    @SerialName("leaderboard")
    LEADERBOARD,

    @SerialName("tournament")
    TOURNAMENT,
}

@Serializable
enum class CategoryName {
    @SerialName("upload_video")
    UPLOAD_VIDEO,

    @SerialName("leaderboard")
    LEADERBOARD,

    @SerialName("tournaments")
    TOURNAMENTS,

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

    @SerialName("chatbot")
    CHAT,
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

    @SerialName("follow")
    FOLLOW,

    @SerialName("unfollow")
    UNFOLLOW,
}

@Serializable
enum class TokenType {
    @SerialName("cents")
    CENTS,

    @SerialName("sats")
    SATS,

    @SerialName("yral")
    YRAL,
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

@Serializable
enum class VideoCreationType {
    @SerialName("ai_video")
    AI_VIDEO,

    @SerialName("upload_video")
    UPLOAD_VIDEO,
}

@Serializable
enum class AiVideoGenFailureType {
    @SerialName("trigger_failed")
    TRIGGER_FAILED,

    @SerialName("generation_failed")
    GENERATION_FAILED,
}

@Serializable
enum class LeaderBoardTabType {
    @SerialName("daily")
    DAILY,

    @SerialName("all")
    ALL,
}

@Serializable
enum class FeedType {
    @SerialName("default")
    DEFAULT,

    @SerialName("ai")
    AI,

    @SerialName("nsfw")
    NSFW,
}

@Serializable
enum class EditProfileSource {
    @SerialName("settings")
    SETTINGS,

    @SerialName("profile")
    PROFILE,
}

@Serializable
enum class FollowersListTab {
    @SerialName("following")
    FOLLOWING,

    @SerialName("followers")
    FOLLOWERS,
}

// --- Tournament ---
@Serializable
data class TournamentScreenViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_SCREEN_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentRegistrationInitiatedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_REGISTRATION_INITIATED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("entry_fee_points") val entryFeePoints: Int,
    @SerialName("user_point_balance") val userPointBalance: Int,
    @SerialName("tournament_duration") val tournamentDuration: Int,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentRegisteredEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_REGISTERED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("entry_fee_points") val entryFeePoints: Int?,
    @SerialName("entry_fee_credits") val entryFeeCredits: Int?,
    @SerialName("registration_time") val registrationTime: String,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentStateChangedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_STATE_CHANGED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("from_state") val fromState: TournamentState,
    @SerialName("to_state") val toState: TournamentState,
    @SerialName("tokens_required") val tokensRequired: Int? = null,
    @SerialName("user_diamonds") val userDiamonds: Int? = null,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentJoinedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_JOINED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("join_time") val joinTime: String,
    @SerialName("diamonds_allocated") val diamondsAllocated: Int,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentAnswerSubmittedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_ANSWER_SUBMITTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("answer_result") val answerResult: TournamentAnswerResult,
    @SerialName("score_delta") val scoreDelta: Int,
    @SerialName("diamonds_remaining") val diamondsRemaining: Int,
    @SerialName("session_id") val sessionId: String,
    @SerialName("emoji_shown") val emojiShown: List<String>,
    @SerialName("user_response") val userResponse: String,
    @SerialName("ai_response") val aiResponse: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentExitAttemptedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_EXIT_ATTEMPTED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("diamonds_remaining") val diamondsRemaining: Int,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentExitNudgeShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_EXIT_NUDGE_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("nudge_type") val nudgeType: String = "Exit Warning",
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentExitConfirmedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_EXIT_CONFIRMED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("diamonds_remaining") val diamondsRemaining: Int,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentOutOfDiamondsShownEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_OUT_OF_DIAMONDS_SHOWN.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("diamonds_remaining") val diamondsRemaining: Int = 0,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentEndedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_ENDED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("tournament_name") val tournamentName: String,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentResultScreenViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_RESULT_SCREEN_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("result") val result: TournamentResult,
    @SerialName("final_score") val finalScore: Int,
    @SerialName("rank") val rank: Int,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentLeaderboardViewedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_LEADERBOARD_VIEWED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("user_rank") val userRank: Int,
    @SerialName("is_winner") val isWinner: Boolean,
    @SerialName("session_id") val sessionId: String,
) : BaseEventData(),
    EventData

@Serializable
data class TournamentRewardEarnedEventData(
    @SerialName("event") override val event: String = FeatureEvents.TOURNAMENT_REWARD_EARNED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.TOURNAMENT.getFeatureName(),
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("tournament_type") val tournamentType: AnalyticsTournamentType,
    @SerialName("reward_amount_inr") val rewardAmountInr: Int,
    @SerialName("reward_currency") val rewardCurrency: String = "BTC",
    @SerialName("rank") val rank: Int,
) : BaseEventData(),
    EventData

@Serializable
enum class TournamentState {
    @SerialName("registration_required")
    REGISTRATION_REQUIRED,

    @SerialName("registered")
    REGISTERED,

    @SerialName("join_now")
    JOIN_NOW,

    @SerialName("join_now_with_tokens")
    JOIN_NOW_WITH_TOKENS,

    @SerialName("join_now_with_credit")
    JOIN_NOW_WITH_CREDIT,

    @SerialName("join_now_disabled")
    JOIN_NOW_DISABLED,

    @SerialName("ended")
    ENDED,
}

@Serializable
enum class TournamentAnswerResult {
    @SerialName("right")
    RIGHT,

    @SerialName("wrong")
    WRONG,
}

@Serializable
enum class AnalyticsTournamentType {
    @SerialName("smiley")
    SMILEY,

    @SerialName("hot_or_not")
    HOT_OR_NOT,
}

@Serializable
enum class TournamentResult {
    @SerialName("win")
    WIN,

    @SerialName("lose")
    LOSE,
}

@Serializable
enum class AnalyticsAlertsRequestType {
    @SerialName("follow_back")
    FOLLOW_BACK,

    @SerialName("video")
    VIDEO,

    @SerialName("default")
    DEFAULT,

    @SerialName("tournament")
    TOURNAMENT,
}

@Serializable
enum class AnalyticsOnboardingStep {
    @SerialName("game_intro_start")
    INTRO_GAME,

    // Step 1
    @SerialName("balance_intro")
    INTRO_BALANCE,

    // Step 2
    @SerialName("rank_intro")
    INTRO_RANK,

    // Step 3
    @SerialName("game_intro_end")
    INTRO_GAME_END, // Step 4
}
