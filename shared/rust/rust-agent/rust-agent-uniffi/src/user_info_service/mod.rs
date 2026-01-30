use candid::{CandidType, Deserialize, Principal};
use ic_agent::Agent;
use std::sync::Arc;
use uniffi::{Enum, Record};

use crate::individual_user_template::individual_user_template_helper::delegated_identity_from_bytes;
use crate::uni_ffi_helpers::FFIError;
use crate::RUNTIME;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISFollowerItem {
    pub caller_follows: bool,
    pub profile_picture_url: Option<String>,
    pub principal_id: Principal,
}

impl From<yral_canisters_client::user_info_service::FollowerItem> for UISFollowerItem {
    fn from(value: yral_canisters_client::user_info_service::FollowerItem) -> Self {
        Self {
            caller_follows: value.caller_follows,
            profile_picture_url: value.profile_picture_url,
            principal_id: value.principal_id,
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISFollowersResponse {
    pub next_cursor: Option<Principal>,
    pub followers: Vec<UISFollowerItem>,
    pub total_count: u64,
}

impl From<yral_canisters_client::user_info_service::FollowersResponse> for UISFollowersResponse {
    fn from(value: yral_canisters_client::user_info_service::FollowersResponse) -> Self {
        Self {
            next_cursor: value.next_cursor,
            followers: value
                .followers
                .into_iter()
                .map(UISFollowerItem::from)
                .collect(),
            total_count: value.total_count,
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISFollowingResponse {
    pub next_cursor: Option<Principal>,
    pub following: Vec<UISFollowerItem>,
    pub total_count: u64,
}

impl From<yral_canisters_client::user_info_service::FollowingResponse> for UISFollowingResponse {
    fn from(value: yral_canisters_client::user_info_service::FollowingResponse) -> Self {
        Self {
            next_cursor: value.next_cursor,
            following: value
                .following
                .into_iter()
                .map(UISFollowerItem::from)
                .collect(),
            total_count: value.total_count,
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISUserProfileGlobalStats {
    pub hot_bets_received: u64,
    pub not_bets_received: u64,
}

impl From<yral_canisters_client::user_info_service::UserProfileGlobalStats>
    for UISUserProfileGlobalStats
{
    fn from(value: yral_canisters_client::user_info_service::UserProfileGlobalStats) -> Self {
        Self {
            hot_bets_received: value.hot_bets_received,
            not_bets_received: value.not_bets_received,
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISUserProfileDetailsForFrontendV3 {
    pub profile_picture_url: Option<String>,
    pub principal_id: Principal,
    pub profile_stats: UISUserProfileGlobalStats,
}

impl From<yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV3>
    for UISUserProfileDetailsForFrontendV3
{
    fn from(
        value: yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV3,
    ) -> Self {
        Self {
            profile_picture_url: value.profile_picture_url,
            principal_id: value.principal_id,
            profile_stats: value.profile_stats.into(),
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISUserProfileDetailsForFrontendV4 {
    pub bio: Option<String>,
    pub website_url: Option<String>,
    pub following_count: u64,
    pub user_follows_caller: Option<bool>,
    pub profile_picture_url: Option<String>,
    pub principal_id: Principal,
    pub profile_stats: UISUserProfileGlobalStats,
    pub followers_count: u64,
    pub caller_follows_user: Option<bool>,
}

impl From<yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV4>
    for UISUserProfileDetailsForFrontendV4
{
    fn from(
        value: yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV4,
    ) -> Self {
        Self {
            bio: value.bio,
            website_url: value.website_url,
            following_count: value.following_count,
            user_follows_caller: value.user_follows_caller,
            profile_picture_url: value.profile_picture_url,
            principal_id: value.principal_id,
            profile_stats: value.profile_stats.into(),
            followers_count: value.followers_count,
            caller_follows_user: value.caller_follows_user,
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone, Debug, PartialEq)]
pub struct UISYralProSubscription {
    pub free_video_credits_left: u32,
    pub total_video_credits_alloted: u32,
}

impl From<yral_canisters_client::user_info_service::YralProSubscription> for UISYralProSubscription {
    fn from(value: yral_canisters_client::user_info_service::YralProSubscription) -> Self {
        Self {
            free_video_credits_left: value.free_video_credits_left,
            total_video_credits_alloted: value.total_video_credits_alloted,
        }
    }
}

#[derive(CandidType, Deserialize, Enum, Clone, Debug, PartialEq)]
pub enum UISSubscriptionPlan {
    Pro(UISYralProSubscription),
    Free,
}

impl From<yral_canisters_client::user_info_service::SubscriptionPlan> for UISSubscriptionPlan {
    fn from(value: yral_canisters_client::user_info_service::SubscriptionPlan) -> Self {
        match value {
            yral_canisters_client::user_info_service::SubscriptionPlan::Free => {
                UISSubscriptionPlan::Free
            }
            yral_canisters_client::user_info_service::SubscriptionPlan::Pro(pro_sub) => {
                UISSubscriptionPlan::Pro(pro_sub.into())
            }
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISUserProfileDetailsForFrontendV6 {
    pub bio: Option<String>,
    pub website_url: Option<String>,
    pub following_count: u64,
    pub user_follows_caller: Option<bool>,
    pub profile_picture: Option<UISProfilePictureData>,
    pub principal_id: Principal,
    pub followers_count: u64,
    pub caller_follows_user: Option<bool>,
    pub subscription_plan: UISSubscriptionPlan,
    pub is_ai_influencer: bool,
}

impl From<yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV6>
    for UISUserProfileDetailsForFrontendV6
{
    fn from(
        value: yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV6,
    ) -> Self {
        Self {
            bio: value.bio,
            website_url: value.website_url,
            following_count: value.following_count,
            user_follows_caller: value.user_follows_caller,
            profile_picture: value.profile_picture.map(|pic| pic.into()),
            principal_id: value.principal_id,
            followers_count: value.followers_count,
            caller_follows_user: value.caller_follows_user,
            subscription_plan: value.subscription_plan.into(),
            is_ai_influencer: value.is_ai_influencer,
        }
    }
}

#[derive(CandidType, Deserialize, Enum, Clone, Debug, PartialEq)]
pub enum UISUserAccountType {
    MainAccount { bots: Vec<Principal> },
    BotAccount { owner: Principal },
}

impl From<yral_canisters_client::user_info_service::UserAccountType> for UISUserAccountType {
    fn from(value: yral_canisters_client::user_info_service::UserAccountType) -> Self {
        match value {
            yral_canisters_client::user_info_service::UserAccountType::MainAccount { bots } => {
                UISUserAccountType::MainAccount { bots }
            }
            yral_canisters_client::user_info_service::UserAccountType::BotAccount { owner } => {
                UISUserAccountType::BotAccount { owner }
            }
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISUserProfileDetailsForFrontendV7 {
    pub bio: Option<String>,
    pub website_url: Option<String>,
    pub is_ai_influencer: bool,
    pub profile_picture: Option<UISProfilePictureData>,
    pub following_count: u64,
    pub user_follows_caller: Option<bool>,
    pub subscription_plan: UISSubscriptionPlan,
    pub principal_id: Principal,
    pub followers_count: u64,
    pub caller_follows_user: Option<bool>,
    pub account_type: UISUserAccountType,
}

impl From<yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV7>
    for UISUserProfileDetailsForFrontendV7
{
    fn from(
        value: yral_canisters_client::user_info_service::UserProfileDetailsForFrontendV7,
    ) -> Self {
        Self {
            bio: value.bio,
            website_url: value.website_url,
            is_ai_influencer: value.is_ai_influencer,
            profile_picture: value.profile_picture.map(|pic| pic.into()),
            following_count: value.following_count,
            user_follows_caller: value.user_follows_caller,
            subscription_plan: value.subscription_plan.into(),
            principal_id: value.principal_id,
            followers_count: value.followers_count,
            caller_follows_user: value.caller_follows_user,
            account_type: value.account_type.into(),
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISNsfwInfo {
    pub is_nsfw: bool,
    pub nsfw_ec: String,
    pub nsfw_gore: String,
    pub csam_detected: bool,
}

impl From<yral_canisters_client::user_info_service::NsfwInfo> for UISNsfwInfo {
    fn from(value: yral_canisters_client::user_info_service::NsfwInfo) -> Self {
        Self {
            is_nsfw: value.is_nsfw,
            nsfw_ec: value.nsfw_ec,
            nsfw_gore: value.nsfw_gore,
            csam_detected: value.csam_detected,
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISProfilePictureData {
    pub url: String,
    pub nsfw_info: UISNsfwInfo,
}

impl From<yral_canisters_client::user_info_service::ProfilePictureData> for UISProfilePictureData {
    fn from(value: yral_canisters_client::user_info_service::ProfilePictureData) -> Self {
        Self {
            url: value.url,
            nsfw_info: value.nsfw_info.into(),
        }
    }
}

impl From<UISNsfwInfo> for yral_canisters_client::user_info_service::NsfwInfo {
    fn from(value: UISNsfwInfo) -> Self {
        Self {
            is_nsfw: value.is_nsfw,
            nsfw_ec: value.nsfw_ec,
            nsfw_gore: value.nsfw_gore,
            csam_detected: value.csam_detected,
        }
    }
}

impl From<UISProfilePictureData> for yral_canisters_client::user_info_service::ProfilePictureData {
    fn from(value: UISProfilePictureData) -> Self {
        Self {
            url: value.url,
            nsfw_info: value.nsfw_info.into(),
        }
    }
}

#[derive(CandidType, Deserialize, Enum, Clone)]
pub enum UISSessionType {
    AnonymousSession,
    RegisteredSession,
}

impl From<yral_canisters_client::user_info_service::SessionType> for UISSessionType {
    fn from(value: yral_canisters_client::user_info_service::SessionType) -> Self {
        match value {
            yral_canisters_client::user_info_service::SessionType::AnonymousSession => {
                UISSessionType::AnonymousSession
            }
            yral_canisters_client::user_info_service::SessionType::RegisteredSession => {
                UISSessionType::RegisteredSession
            }
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISProfileUpdateDetails {
    pub bio: Option<String>,
    pub website_url: Option<String>,
    pub profile_picture_url: Option<String>,
}

impl From<UISProfileUpdateDetails> for yral_canisters_client::user_info_service::ProfileUpdateDetails {
    fn from(value: UISProfileUpdateDetails) -> Self {
        Self {
            bio: value.bio,
            website_url: value.website_url,
            profile_picture_url: value.profile_picture_url,
        }
    }
}

#[derive(CandidType, Deserialize, Record, Clone)]
pub struct UISProfileUpdateDetailsV2 {
    pub bio: Option<String>,
    pub website_url: Option<String>,
    pub profile_picture: Option<UISProfilePictureData>,
}

impl From<UISProfileUpdateDetailsV2> for yral_canisters_client::user_info_service::ProfileUpdateDetailsV2 {
    fn from(value: UISProfileUpdateDetailsV2) -> Self {
        Self {
            bio: value.bio,
            website_url: value.website_url,
            profile_picture: value.profile_picture.map(|pic| pic.into()),
        }
    }
}

#[derive(uniffi::Object)]
pub struct UserInfoService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl UserInfoService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        identity_data: Vec<u8>,
    ) -> std::result::Result<UserInfoService, FFIError> {
        let principal = Principal::from_text(principal_text)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        let identity = delegated_identity_from_bytes(&identity_data.as_slice())
            .map_err(|e| FFIError::UnknownError(format!("Invalid identity: {:?}", e)))?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .with_identity(identity)
            .build()
            .map_err(|e| FFIError::AgentError(format!("Failed to create agent: {:?}", e)))?;

        Ok(UserInfoService {
            principal,
            agent: Arc::new(agent),
        })
    }

    #[uniffi::method]
    pub async fn follow_user(&self, target_principal_text: String) -> Result<()> {
        let agent = Arc::clone(&self.agent);
        RUNTIME
            .spawn(async move {
                let target = Principal::from_text(target_principal_text)
                    .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .follow_user(target)
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result_::Ok => Ok(()),
                    yral_canisters_client::user_info_service::Result_::Err(msg) => {
                        Err(FFIError::UnknownError(format!("Follow failed: {}", msg)))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn unfollow_user(&self, target_principal_text: String) -> Result<()> {
        let agent = Arc::clone(&self.agent);
        RUNTIME
            .spawn(async move {
                let target = Principal::from_text(target_principal_text)
                    .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .unfollow_user(target)
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result_::Ok => Ok(()),
                    yral_canisters_client::user_info_service::Result_::Err(msg) => {
                        Err(FFIError::UnknownError(format!("Unfollow failed: {}", msg)))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_profile_details_v4(
        &self,
        principal_text: String,
    ) -> Result<UISUserProfileDetailsForFrontendV4> {
        let agent = Arc::clone(&self.agent);
        RUNTIME
            .spawn(async move {
                let principal = Principal::from_text(principal_text)
                    .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .get_profile_details_v_4(principal)
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result3::Ok(details) => {
                        Ok(details.into())
                    }
                    yral_canisters_client::user_info_service::Result3::Err(msg) => {
                        Err(FFIError::UnknownError(format!("{:?}", msg)))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_user_profile_details_v6(
        &self,
        principal_text: String,
    ) -> Result<UISUserProfileDetailsForFrontendV6> {
        let agent = Arc::clone(&self.agent);
        RUNTIME
            .spawn(async move {
                let principal = Principal::from_text(principal_text)
                    .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .get_user_profile_details_v_6(principal)
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result6::Ok(details) => {
                        Ok(details.into())
                    }
                    yral_canisters_client::user_info_service::Result6::Err(msg) => {
                        Err(FFIError::UnknownError(format!("{:?}", msg)))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_user_profile_details_v7(
        &self,
        principal_text: String,
    ) -> Result<UISUserProfileDetailsForFrontendV7> {
        let agent = Arc::clone(&self.agent);
        RUNTIME
            .spawn(async move {
                let principal = Principal::from_text(principal_text)
                    .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .get_user_profile_details_v_7(principal)
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result7::Ok(details) => {
                        Ok(details.into())
                    }
                    yral_canisters_client::user_info_service::Result7::Err(msg) => {
                        Err(FFIError::UnknownError(format!("{:?}", msg)))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn update_profile_details(&self, details: UISProfileUpdateDetails) -> Result<()> {
        let agent = Arc::clone(&self.agent);
        let update_details = details;
        RUNTIME
            .spawn(async move {
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .update_profile_details(update_details.into())
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result_::Ok => Ok(()),
                    yral_canisters_client::user_info_service::Result_::Err(msg) => {
                        Err(FFIError::UnknownError(format!(
                            "Update profile details failed: {}",
                            msg
                        )))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn update_profile_details_v2(&self, details: UISProfileUpdateDetailsV2) -> Result<()> {
        let agent = Arc::clone(&self.agent);
        let update_details = details;
        RUNTIME
            .spawn(async move {
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .update_profile_details_v_2(update_details.into())
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result_::Ok => Ok(()),
                    yral_canisters_client::user_info_service::Result_::Err(msg) => {
                        Err(FFIError::UnknownError(format!(
                            "Update profile details failed: {}",
                            msg
                        )))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_followers(
        &self,
        principal_text: String,
        cursor_principal_text: Option<String>,
        limit: u64,
        with_caller_follows: Option<bool>,
    ) -> Result<UISFollowersResponse> {
        let agent = Arc::clone(&self.agent);
        RUNTIME
            .spawn(async move {
                let principal = Principal::from_text(principal_text)
                    .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
                let cursor_opt = match cursor_principal_text {
                    Some(text) => Some(
                        Principal::from_text(text)
                            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?,
                    ),
                    None => None,
                };
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .get_followers(principal, cursor_opt, limit, with_caller_follows)
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result1::Ok(details) => {
                        Ok(details.into())
                    }
                    yral_canisters_client::user_info_service::Result1::Err(msg) => {
                        Err(FFIError::UnknownError(format!("{:?}", msg)))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_following(
        &self,
        principal_text: String,
        cursor_principal_text: Option<String>,
        limit: u64,
        with_caller_follows: Option<bool>,
    ) -> Result<UISFollowingResponse> {
        let agent = Arc::clone(&self.agent);
        RUNTIME
            .spawn(async move {
                let principal = Principal::from_text(principal_text)
                    .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
                let cursor_opt = match cursor_principal_text {
                    Some(text) => Some(
                        Principal::from_text(text)
                            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?,
                    ),
                    None => None,
                };
                let service = yral_canisters_client::user_info_service::UserInfoService(
                    yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                    &agent,
                );
                let res = service
                    .get_following(principal, cursor_opt, limit, with_caller_follows)
                    .await
                    .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match res {
                    yral_canisters_client::user_info_service::Result2::Ok(details) => {
                        Ok(details.into())
                    }
                    yral_canisters_client::user_info_service::Result2::Err(msg) => {
                        Err(FFIError::UnknownError(format!("{:?}", msg)))
                    }
                }
            })
            .await
            .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }
}
