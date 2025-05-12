// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
use crate::individual_user_template;
use crate::RUNTIME;
use crate::uni_ffi_helpers::*;
use candid::{self, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::Agent;
use std::sync::Arc;
use uniffi::Record;
use uniffi::Enum;
use crate::commons::*;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct PostCacheInitArgs {
    pub known_principal_ids: Option<Vec<KnownPrincipalTypePrincipalPair>>,
    pub version: String,
    pub upgrade_version_number: Option<u64>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum NsfwFilter {
    IncludeNsfw,
    OnlyNsfw,
    ExcludeNsfw,
}

#[derive(CandidType, Deserialize, Record)]
pub struct PostScoreIndexItemV1 {
    pub is_nsfw: bool,
    pub status: PostStatus,
    pub post_id: u64,
    pub created_at: Option<SystemTime>,
    pub score: u64,
    pub publisher_canister_id: Principal,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum TopPostsFetchError {
    ReachedEndOfItemsList,
    InvalidBoundsPassed,
    ExceededMaxNumberOfItemsAllowedInOneRequest,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum PostCacheResult_ {
    Ok(Vec<PostScoreIndexItemV1>),
    Err(TopPostsFetchError),
}

#[derive(uniffi::Object)]
pub struct PostCacheService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl PostCacheService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<PostCacheService, FFIError> {
        let principal = Principal::from_text(principal_text)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .build()
            .map_err(|e| FFIError::AgentError(format!("Failed to create agent: {:?}", e)))?;
        RUNTIME
            .block_on(agent.fetch_root_key())
            .map_err(|e| FFIError::UnknownError(format!("Failed to fetch root key: {:?}", e)))?;
        Ok(Self {
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
    pub async fn get_cycle_balance(&self) -> Result<candid::Nat> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_cycle_balance", args).await?;
        Ok(Decode!(&bytes, candid::Nat)?)
    }

    #[uniffi::method]
    pub async fn get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor(
        &self,
        arg0: u64,
        arg1: u64,
        arg2: Option<bool>,
        arg3: Option<PostStatus>,
        arg4: Option<NsfwFilter>,
    ) -> Result<PostCacheResult_> {
        let args = Encode!(&arg0, &arg1, &arg2, &arg3, &arg4)?;
        let bytes = self.query_canister(
            "get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor",
            args,
        ).await?;
        Ok(Decode!(&bytes, PostCacheResult_)?)
    }

    #[uniffi::method]
    pub async fn get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor(
        &self,
        arg0: u64,
        arg1: u64,
        arg2: Option<bool>,
        arg3: Option<PostStatus>,
        arg4: Option<NsfwFilter>,
    ) -> Result<PostCacheResult_> {
        let args = Encode!(&arg0, &arg1, &arg2, &arg3, &arg4)?;
        let bytes = self.query_canister(
            "get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor",
            args,
        ).await?;
        Ok(Decode!(&bytes, PostCacheResult_)?)
    }

    #[uniffi::method]
    pub async fn get_well_known_principal_value(
        &self,
        arg0: KnownPrincipalType,
    ) -> Result<Option<Principal>> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_well_known_principal_value", args).await?;
        Ok(Decode!(&bytes, Option<Principal>)?)
    }

    #[uniffi::method]
    pub async fn http_request(&self, arg0: HttpRequest) -> Result<HttpResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("http_request", args).await?;
        Ok(Decode!(&bytes, HttpResponse)?)
    }

    #[uniffi::method]
    pub async fn receive_top_home_feed_posts_from_publishing_canister(
        &self,
        arg0: Vec<PostScoreIndexItemV1>,
    ) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister(
            "receive_top_home_feed_posts_from_publishing_canister",
            args,
        ).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn receive_top_hot_or_not_feed_posts_from_publishing_canister(
        &self,
        arg0: Vec<PostScoreIndexItemV1>,
    ) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister(
            "receive_top_hot_or_not_feed_posts_from_publishing_canister",
            args,
        ).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn remove_all_feed_entries(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self.update_canister("remove_all_feed_entries", args).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn update_post_home_feed(&self, arg0: PostScoreIndexItemV1) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_post_home_feed", args).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn update_post_hot_or_not_feed(&self, arg0: PostScoreIndexItemV1) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_post_hot_or_not_feed", args).await?;
        Ok(Decode!(&bytes)?)
    }
}
