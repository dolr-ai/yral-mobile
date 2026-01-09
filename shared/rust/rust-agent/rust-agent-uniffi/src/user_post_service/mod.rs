use candid::{Principal, CandidType, Deserialize};
use ic_agent::Agent;
use std::sync::Arc;
use uniffi::Record;
use uniffi::Enum;
use crate::individual_user_template::individual_user_template_helper::*;
use crate::uni_ffi_helpers::*;
use crate::RUNTIME;
use crate::commons::SystemTime;

use yral_canisters_client::user_post_service::Result2;
use yral_canisters_client::user_post_service::Post;
use yral_canisters_client::user_post_service::PostStatus;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(uniffi::Object)]
pub struct ServiceCanistersDetails {
    pub user_post_cansiter_id: Principal,
    pub user_info_canister_id: Principal,
}

#[uniffi::export]
impl ServiceCanistersDetails {
    #[uniffi::constructor]
    pub fn new() -> ServiceCanistersDetails {
        ServiceCanistersDetails {
            user_post_cansiter_id: yral_canisters_client::ic::USER_POST_SERVICE_ID,
            user_info_canister_id: yral_canisters_client::ic::USER_INFO_SERVICE_ID,
        }
    }

    #[uniffi::method]
    pub fn get_user_post_service_canister_id(&self) -> Principal {
        self.user_post_cansiter_id
    }

    #[uniffi::method]
    pub fn get_user_info_service_canister_id(&self) -> Principal {
        self.user_info_canister_id
    }
}

#[derive(CandidType, Deserialize, Enum)]
pub enum UPSResult3 {
    Ok(Vec<UPSPostDetailsForFrontend>),
    Err(String),
}

impl From<yral_canisters_client::user_post_service::Result3> for UPSResult3 {
    fn from(result: yral_canisters_client::user_post_service::Result3) -> Self {
        match result {
            yral_canisters_client::user_post_service::Result3::Ok(post_details) => {
                UPSResult3::Ok(vec![UPSPostDetailsForFrontend::from_post_details(post_details)])
            }
            yral_canisters_client::user_post_service::Result3::Err(err) => {
                UPSResult3::Err(format!("{:?}", err))
            }
        }
    }
}

impl From<yral_canisters_client::user_post_service::Result1> for UPSResult3 {
    fn from(result: yral_canisters_client::user_post_service::Result1) -> Self {
        match result {
            yral_canisters_client::user_post_service::Result1::Ok(posts) => UPSResult3::Ok(
                posts
                    .into_iter()
                    .map(|post| UPSPostDetailsForFrontend::from_post(post, &Principal::anonymous()))
                    .collect(),
            ),
            yral_canisters_client::user_post_service::Result1::Err(err) => {
                UPSResult3::Err(format!("{:?}", err))
            }
        }
    }
}

#[derive(CandidType, Deserialize, Enum)]
pub enum UPSGetPostsOfUserProfileError {
    ReachedEndOfItemsList,
    InvalidBoundsPassed,
    ExceededMaxNumberOfItemsAllowedInOneRequest,
}

impl From<yral_canisters_client::user_post_service::GetPostsOfUserProfileError> for UPSGetPostsOfUserProfileError {
    fn from(error: yral_canisters_client::user_post_service::GetPostsOfUserProfileError) -> Self {
        match error {
            yral_canisters_client::user_post_service::GetPostsOfUserProfileError::ReachedEndOfItemsList => UPSGetPostsOfUserProfileError::ReachedEndOfItemsList,
            yral_canisters_client::user_post_service::GetPostsOfUserProfileError::InvalidBoundsPassed => UPSGetPostsOfUserProfileError::InvalidBoundsPassed,
            yral_canisters_client::user_post_service::GetPostsOfUserProfileError::ExceededMaxNumberOfItemsAllowedInOneRequest => UPSGetPostsOfUserProfileError::ExceededMaxNumberOfItemsAllowedInOneRequest,
        }
    }
}

#[derive(CandidType, Deserialize, Record)]
pub struct UPSPostDetailsForFrontend{
    pub id: String,
    pub status: UPSPostStatus,
    pub hashtags: Vec<String>,
    pub like_count: u64,
    pub description: String,
    pub total_view_count: u64,
    pub created_at: SystemTime,
    pub video_uid: String,
    pub created_by_user_principal_id: Principal,
    pub creator_principal: Principal,
    pub liked_by_me: bool,
}

impl UPSPostDetailsForFrontend {
    pub fn from_post(post: Post, current_user: &Principal) -> Self {
        Self {
            id: post.id,
            status: UPSPostStatus::from(post.status),
            hashtags: post.hashtags,
            like_count: post.likes.len() as u64,
            description: post.description,
            total_view_count: post.view_stats.total_view_count,
            created_at: SystemTime::from(post.created_at),
            video_uid: post.video_uid,
            created_by_user_principal_id: post.creator_principal.clone(),
            creator_principal: post.creator_principal,
            liked_by_me: post.likes.contains(current_user),
        }
    }

    pub fn from_post_details(details: yral_canisters_client::user_post_service::PostDetailsForFrontend) -> Self {
        Self {
            id: details.id,
            status: UPSPostStatus::from(yral_canisters_client::user_post_service::PostStatus::ReadyToView),
            hashtags: details.hashtags,
            like_count: details.like_count,
            description: details.description,
            total_view_count: details.total_view_count,
            created_at: SystemTime::from(details.created_at),
            video_uid: details.video_uid,
            created_by_user_principal_id: details.created_by_user_principal_id,
            creator_principal: details.creator_principal,
            liked_by_me: details.liked_by_me,
        }
    }
}

#[derive(CandidType, Deserialize, Enum)]
pub enum UPSPostStatus {
    BannedForExplicitness,
    BannedDueToUserReporting,
    Uploaded,
    Draft,
    CheckingExplicitness,
    ReadyToView,
    Transcoding,
    Deleted,
}

impl From<PostStatus> for UPSPostStatus {
    fn from(status: PostStatus) -> Self {
        match status {
            PostStatus::BannedForExplicitness => UPSPostStatus::BannedForExplicitness,
            PostStatus::BannedDueToUserReporting => UPSPostStatus::BannedDueToUserReporting,
            PostStatus::Uploaded => UPSPostStatus::Uploaded,
            PostStatus::Draft => UPSPostStatus::Draft,
            PostStatus::CheckingExplicitness => UPSPostStatus::CheckingExplicitness,
            PostStatus::ReadyToView => UPSPostStatus::ReadyToView,
            PostStatus::Transcoding => UPSPostStatus::Transcoding,
            PostStatus::Deleted => UPSPostStatus::Deleted,
        }
    }
}

impl From<yral_canisters_client::user_post_service::SystemTime> for SystemTime {
    fn from(value: yral_canisters_client::user_post_service::SystemTime) -> Self {
        Self {
            nanos_since_epoch: value.nanos_since_epoch,
            secs_since_epoch: value.secs_since_epoch,
        }
    }
}

#[derive(uniffi::Object)]
pub struct UserPostService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl UserPostService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        identity_data: Vec<u8>,
    ) -> std::result::Result<UserPostService, FFIError> {
        let principal = Principal::from_text(principal_text)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        let identity = delegated_identity_from_bytes(&identity_data.as_slice())
            .map_err(|e| FFIError::UnknownError(format!("Invalid identity: {:?}", e)))?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .with_identity(identity)
            .build()
            .map_err(|e| FFIError::AgentError(format!("Failed to create agent: {:?}", e)))?;
    
        Ok(UserPostService {
            principal,
            agent: Arc::new(agent),
        })
    }

    #[uniffi::method]
    pub async fn get_individual_post_details_by_id(
        &self,
        arg0: String,
    ) -> Result<UPSPostDetailsForFrontend> {
        let agent = Arc::clone(&self.agent);
        let principal = self.principal;
        RUNTIME.spawn(async move {
            let service = yral_canisters_client::user_post_service::UserPostService(
                yral_canisters_client::ic::USER_POST_SERVICE_ID,
                &agent,
            );
            let details = service
                .get_individual_post_details_by_id(arg0)
                .await
                .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
                match details {
                    Result2::Ok(post_details) => {
                        Ok(UPSPostDetailsForFrontend::from_post(post_details, &principal))
                    }
                    Result2::Err(err) => Err(FFIError::UnknownError(format!("Details not found {:?}", err))),
                }
        }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_posts_of_this_user_profile_with_pagination_cursor(
        &self,
        arg0: String,
        arg1: u64,
        arg2: u64,
    ) -> Result<UPSResult3> {
        let agent = Arc::clone(&self.agent);
        let principal = Principal::from_text(arg0)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        RUNTIME.spawn(async move {
            let service = yral_canisters_client::user_post_service::UserPostService(
                yral_canisters_client::ic::USER_POST_SERVICE_ID,
                &agent,
            );
            let details = service
                .get_posts_of_this_user_profile_with_pagination(principal, arg1, arg2)
                .await
                .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
            Ok(UPSResult3::from(details))
        }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }
}
