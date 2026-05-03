#![allow(dead_code, unused_imports)]
pub mod individual_user_template_helper;

use candid::{self, CandidType, Decode, Deserialize, Encode, Nat, Principal};
use ic_agent::identity::Secp256k1Identity;
use ic_agent::{
    export::PrincipalError,
    identity::DelegatedIdentity,
    Agent,
};
use k256::elliptic_curve::JwkEcKey;
use std::sync::Arc;
use serde_bytes::ByteBuf;
use uniffi::Enum;
use uniffi::Record;
use crate::commons::*;
use crate::individual_user_template::individual_user_template_helper::*;
use crate::RUNTIME;
use crate::uni_ffi_helpers::*;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Enum)]
pub enum BettingStatus {
    BettingOpen {
        number_of_participants: u8,
        ongoing_room: u64,
        ongoing_slot: u8,
        has_this_user_participated_in_this_post: Option<bool>,
        started_at: SystemTime,
    },
    BettingClosed,
}
#[derive(CandidType, Deserialize, Record)]
pub struct PostDetailsForFrontend {
    pub id: u64,
    pub is_nsfw: bool,
    pub status: PostStatus,
    pub home_feed_ranking_score: u64,
    pub hashtags: Vec<String>,
    pub hot_or_not_betting_status: Option<BettingStatus>,
    pub like_count: u64,
    pub description: String,
    pub total_view_count: u64,
    pub created_by_display_name: Option<String>,
    pub created_at: SystemTime,
    pub created_by_unique_user_name: Option<String>,
    pub video_uid: String,
    pub created_by_user_principal_id: Principal,
    pub hot_or_not_feed_ranking_score: Option<u64>,
    pub liked_by_me: bool,
    pub created_by_profile_photo_url: Option<String>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum GetPostsOfUserProfileError {
    ReachedEndOfItemsList,
    InvalidBoundsPassed,
    ExceededMaxNumberOfItemsAllowedInOneRequest,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result12 {
    Ok(Vec<PostDetailsForFrontend>),
    Err(GetPostsOfUserProfileError),
}

#[derive(uniffi::Object)]
pub struct IndividualUserService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl IndividualUserService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        identity_data: Vec<u8>,
    ) -> std::result::Result<IndividualUserService, FFIError> {
        let principal = Principal::from_text(principal_text)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        let identity = delegated_identity_from_bytes(&identity_data.as_slice())
            .map_err(|e| FFIError::UnknownError(format!("Invalid: {:?}", e)))?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .with_identity(identity)
            .build()
            .map_err(|e| FFIError::AgentError(format!("Failed to create agent: {:?}", e)))?;

        Ok(IndividualUserService {
            principal,
            agent: Arc::new(agent),
        })
    }

    async fn query_canister(&self, method: &str, args: Vec<u8>) -> Result<Vec<u8>> {
        let agent = Arc::clone(&self.agent);
        let principal = self.principal;
        let method = method.to_string();
        RUNTIME.spawn(async move {
            agent
                .query(&principal, &method)
                .with_arg(args)
                .call()
                .await
                .map_err(|e| FFIError::AgentError(format!("{:?}", e)))
        })
            .await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    async fn update_canister(&self, method: &str, args: Vec<u8>) -> Result<Vec<u8>> {
        let agent = Arc::clone(&self.agent);
        let principal = self.principal;
        let method = method.to_string();
        RUNTIME.spawn(async move {
            agent
                .update(&principal, &method)
                .with_arg(args)
                .call_and_wait()
                .await
                .map_err(|e| FFIError::AgentError(format!("{:?}", e)))
        })
            .await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    #[uniffi::method]
    pub async fn get_individual_post_details_by_id(
        &self,
        arg0: u64,
    ) -> Result<PostDetailsForFrontend> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_individual_post_details_by_id", args).await?;
        Ok(Decode!(&bytes, PostDetailsForFrontend)?)
    }
    #[uniffi::method]
    pub async fn get_posts_of_this_user_profile_with_pagination_cursor(
        &self,
        arg0: u64,
        arg1: u64,
    ) -> Result<Result12> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_posts_of_this_user_profile_with_pagination_cursor", args).await?;
        Ok(Decode!(&bytes, Result12)?)
    }
}
