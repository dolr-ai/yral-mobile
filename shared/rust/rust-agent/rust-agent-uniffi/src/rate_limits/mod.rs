use candid::{Principal, CandidType, Deserialize};
use ic_agent::Agent;
use std::sync::Arc;
use uniffi::Record;
use uniffi::Enum;
use crate::individual_user_template::individual_user_template_helper::*;
use crate::uni_ffi_helpers::*;
use crate::RUNTIME;

use yral_canisters_client::ic::RATE_LIMITS_ID;
use yral_canisters_client::rate_limits::VideoGenRequestKey;
use yral_canisters_client::rate_limits::Result2;
use yral_canisters_client::rate_limits::VideoGenRequestStatus;
use yral_canisters_client::rate_limits::RateLimitStatus;
use yral_canisters_client::rate_limits::RateLimits;

type Result<T> = std::result::Result<T, FFIError>;


#[derive(CandidType, Deserialize, Record)]
pub struct RateLimitStatusWrapper {
  pub principal: Principal,
  pub window_start: u64,
  pub is_limited: bool,
  pub request_count: u64,
}

impl From<RateLimitStatus> for RateLimitStatusWrapper {
    fn from(value: RateLimitStatus) -> Self {
        Self {
            principal: value.principal,
            window_start: value.window_start,
            is_limited: value.is_limited,
            request_count: value.request_count,
        }
    }
}


#[derive(CandidType, Deserialize, Record)]
pub struct VideoGenRequestKeyWrapper {
    pub principal: Principal,
    pub counter: u64,
}

impl From<VideoGenRequestKeyWrapper> for VideoGenRequestKey {
    fn from(value: VideoGenRequestKeyWrapper) -> Self {
        Self {
            principal: value.principal,
            counter: value.counter,
        }
    }
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Result2Wrapper {
    Ok(VideoGenRequestStatusWrapper),
    Err(String),
}

impl From<Result2> for Result2Wrapper {
    fn from(value: Result2) -> Self {
        match value {
            Result2::Ok(status) => Result2Wrapper::Ok(VideoGenRequestStatusWrapper::from(status)),
            Result2::Err(err) => Result2Wrapper::Err(err),
        }
    }
}

#[derive(CandidType, Deserialize, Enum)]
pub enum VideoGenRequestStatusWrapper {
    Failed(String),
    Complete(String),
    Processing,
    Pending,
}

impl From<VideoGenRequestStatus> for VideoGenRequestStatusWrapper {
    fn from(value: VideoGenRequestStatus) -> Self {
        match value {
            VideoGenRequestStatus::Failed(s) => Self::Failed(s),
            VideoGenRequestStatus::Complete(s) => Self::Complete(s),
            VideoGenRequestStatus::Processing => Self::Processing,
            VideoGenRequestStatus::Pending => Self::Pending,
        }
    }
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

    #[uniffi::method]
    pub async fn poll_video_generation_status(
        &self,
        arg0: VideoGenRequestKeyWrapper,
    ) -> Result<Result2Wrapper> {
        let agent = Arc::clone(&self.agent);
        RUNTIME.spawn(async move {
            let canister = RateLimits(RATE_LIMITS_ID, &agent);
            let video_request_key = VideoGenRequestKey::from(arg0);
            let result = canister
                .poll_video_generation_status(video_request_key)
                .await
                .map_err(|e| FFIError::AgentError(format!("Error calling canister: {:?}", e)))?;
            Ok(Result2Wrapper::from(result))
        }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }

    
    #[uniffi::method]
    pub async fn get_rate_limit_status(
        &self,
        arg1: String, 
        arg2: bool,
    ) -> Result<Option<RateLimitStatusWrapper>> {
        let agent = Arc::clone(&self.agent);
        let principal = self.principal;
        RUNTIME.spawn(async move {
            let canister = RateLimits(RATE_LIMITS_ID, &agent);
            let status_opt = canister
                .get_rate_limit_status(principal, arg1, arg2)
                .await
                .map_err(|e| FFIError::AgentError(format!("Error calling canister: {:?}", e)))?;
            Ok(status_opt.map(RateLimitStatusWrapper::from))
        }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
    }
}
