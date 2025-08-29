use candid::{Principal, CandidType, Deserialize};
use ic_agent::Agent;
use std::sync::Arc;
use uniffi::Record;
use crate::individual_user_template::individual_user_template_helper::*;
use crate::uni_ffi_helpers::*;
use crate::RUNTIME;
use crate::commons::SystemTime;

use yral_canisters_client::user_post_service::Result1;
use yral_canisters_client::user_post_service::Post;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct SCPostDetailsForFrontend{
    pub id: String,
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

impl SCPostDetailsForFrontend {
    pub fn from_post(post: Post, current_user: &Principal) -> Self {
        Self {
            id: post.id,
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
    ) -> Result<SCPostDetailsForFrontend> {
        let agent = Arc::clone(&self.agent);
        let principal = self.principal;
        RUNTIME.spawn(async move {
            let service = yral_canisters_client::user_post_service::UserPostService(
                yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                &agent,
            );
            let details = service
                .get_individual_post_details_by_id(arg0)
                .await
                .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
            match details {
                Result1::Ok(post_details) => Ok(SCPostDetailsForFrontend::from_post(post_details, &principal)),
                Result1::Err(err) => Err(FFIError::UnknownError(format!("Details not found {:?}", err))),
            }
        }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_posts_of_this_user_profile_with_pagination_cursor(
        &self,
        arg0: String,
        arg1: u64,
        arg2: u64,
    ) -> Result<Vec<SCPostDetailsForFrontend>> {
        let agent = Arc::clone(&self.agent);
        let principal = Principal::from_text(arg0)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        RUNTIME.spawn(async move {
            let service = yral_canisters_client::user_post_service::UserPostService(
                yral_canisters_client::ic::USER_INFO_SERVICE_ID,
                &agent,
            );
            let details = service
                .get_posts_of_this_user_profile_with_pagination_cursor(principal, arg1, arg2)
                .await
                .map_err(|e| FFIError::AgentError(format!("{:?}", e)))?;
            Ok(details.into_iter().map(|post| SCPostDetailsForFrontend::from_post(post, &principal)).collect())
        }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }
}
