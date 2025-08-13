use candid::{self, CandidType, Deserialize, Principal, Encode, Decode};
use uniffi::Enum;
use uniffi::Record;
use ic_agent::Agent;
use std::sync::Arc;
use crate::individual_user_template::individual_user_template_helper::*;
use crate::uni_ffi_helpers::*;
use crate::RUNTIME;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct RateLimitsInitArgs { pub version: String }

#[derive(CandidType, Deserialize, Enum)]
pub enum RateLimitResult { 
    Ok(String), 
    Err(String) 
}

#[derive(CandidType, Deserialize, Record)]
pub struct VideoGenRequestKey { 
    pub principal: Principal, 
    pub counter: u64 
}

#[derive(CandidType, Deserialize, Record)]
pub struct GlobalRateLimitConfig {
  pub window_duration_seconds: u64,
  pub max_requests_per_window_registered: u64,
  pub max_requests_per_window_unregistered: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RateLimitConfig {
  pub window_duration_seconds: u64,
  pub max_requests_per_window: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct PropertyRateLimitConfig {
  pub property_rate_limit_window_duration_seconds: Option<u64>,
  pub window_duration_seconds: u64,
  pub max_requests_per_window_registered: u64,
  pub max_requests_per_property_all_users: Option<u64>,
  pub property: String,
  pub max_requests_per_window_unregistered: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RateLimitStatus {
  pub principal: Principal,
  pub window_start: u64,
  pub is_limited: bool,
  pub request_count: u64,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum VideoGenRequestStatus {
  Failed(String),
  Complete(String),
  Processing,
  Pending,
}

#[derive(CandidType, Deserialize, Record)]
pub struct VideoGenRequest {
  pub status: VideoGenRequestStatus,
  pub updated_at: u64,
  pub payment_amount: Option<String>,
  pub model_name: String,
  pub created_at: u64,
  pub prompt: String,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum PollResult2 { 
    Ok(VideoGenRequestStatus),
    Err(String) 
}

#[derive(uniffi::Object)]
pub struct RateLimitService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl RateLimitService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        identity_data: Vec<u8>,
    ) -> std::result::Result<RateLimitService, FFIError> {
        let principal = Principal::from_text(principal_text)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        let identity = delegated_identity_from_bytes(&identity_data.as_slice())
            .map_err(|e| FFIError::UnknownError(format!("Invalid identity: {:?}", e)))?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .with_identity(identity)
            .build()
            .map_err(|e| FFIError::AgentError(format!("Failed to create agent: {:?}", e)))?;
    
        Ok(RateLimitService {
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
    pub async fn poll_video_generation_status(
        &self,
        arg0: VideoGenRequestKey,
    ) -> Result<PollResult2> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("poll_video_generation_status", args).await?;
        Ok(Decode!(&bytes, PollResult2)?)
    }
    #[uniffi::method]
    pub async fn get_rate_limit_status(
        &self,
        arg0: String, 
        arg1: String, 
        arg2: bool,
    ) -> Result<Option<RateLimitStatus>> {
        let principal = Principal::from_text(&arg0)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        let args = Encode!(&principal, &arg1, &arg2)?;
        let bytes = self.update_canister("get_rate_limit_status", args).await?;
        Ok(Decode!(&bytes, Option<RateLimitStatus>)?)
    }
}
