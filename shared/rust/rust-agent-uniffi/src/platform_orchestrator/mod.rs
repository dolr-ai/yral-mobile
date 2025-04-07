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
use crate::commons::{HttpRequest, HttpResponse};

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct PlatformOrchestratorInitArgs {
    pub version: String,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum PlatformOrchestratorResult_ {
    Ok(String),
    Err(String),
}

#[derive(CandidType, Deserialize, Enum)]
pub enum WasmType {
    IndividualUserWasm,
    PostCacheWasm,
    SubnetOrchestratorWasm,
}

#[derive(CandidType, Deserialize, Record)]
pub struct UpgradeCanisterArg {
    pub version: String,
    pub canister: WasmType,
    pub wasm_blob: serde_bytes::ByteBuf,
}

#[derive(CandidType, Deserialize, Record)]
pub struct CanisterUpgradeStatus {
    pub failures: Vec<PrincipalStringPair>,
    pub count: u64,
    pub upgrade_arg: UpgradeCanisterArg,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum PlatformOrchestratorResult1 {
    Ok(Principal),
    Err(String),
}

#[derive(uniffi::Object)]
pub struct PlatformOrchestratorService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl PlatformOrchestratorService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<PlatformOrchestratorService, FFIError> {
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
    pub async fn deposit_cycles_to_canister(
        &self,
        arg0: Principal,
        arg1: candid::Nat,
    ) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("deposit_cycles_to_canister", args).await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }

    #[uniffi::method]
    pub async fn get_all_available_subnet_orchestrators(&self) -> Result<Vec<Principal>> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_all_available_subnet_orchestrators", args).await?;
        Ok(Decode!(&bytes, Vec<Principal>)?)
    }

    #[uniffi::method]
    pub async fn get_all_subnet_orchestrators(&self) -> Result<Vec<Principal>> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_all_subnet_orchestrators", args).await?;
        Ok(Decode!(&bytes, Vec<Principal>)?)
    }

    #[uniffi::method]
    pub async fn get_subnet_last_upgrade_status(&self) -> Result<CanisterUpgradeStatus> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_subnet_last_upgrade_status", args).await?;
        Ok(Decode!(&bytes, CanisterUpgradeStatus)?)
    }

    #[uniffi::method]
    pub async fn get_version(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_version", args).await?;
        Ok(Decode!(&bytes, String)?)
    }

    #[uniffi::method]
    pub async fn http_request(&self, arg0: HttpRequest) -> Result<HttpResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("http_request", args).await?;
        Ok(Decode!(&bytes, HttpResponse)?)
    }

    #[uniffi::method]
    pub async fn provision_subnet_orchestrator_canister(
        &self,
        arg0: Principal,
    ) -> Result<PlatformOrchestratorResult1> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("provision_subnet_orchestrator_canister", args).await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult1)?)
    }

    #[uniffi::method]
    pub async fn start_reclaiming_cycles_from_individual_canisters(
        &self,
    ) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!()?;
        let bytes = self.update_canister("start_reclaiming_cycles_from_individual_canisters", args).await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }

    #[uniffi::method]
    pub async fn start_reclaiming_cycles_from_subnet_orchestrator_canister(
        &self,
    ) -> Result<String> {
        let args = Encode!()?;
        let bytes = self.update_canister("start_reclaiming_cycles_from_subnet_orchestrator_canister", args).await?;
        Ok(Decode!(&bytes, String)?)
    }

    #[uniffi::method]
    pub async fn stop_upgrades_for_individual_user_canisters(
        &self,
    ) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!()?;
        let bytes = self.update_canister("stop_upgrades_for_individual_user_canisters", args).await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }

    #[uniffi::method]
    pub async fn subnet_orchestrator_maxed_out(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self.update_canister("subnet_orchestrator_maxed_out", args).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn update_profile_owner_for_individual_canisters(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self.update_canister("update_profile_owner_for_individual_canisters", args).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn upgrade_canister(
        &self,
        arg0: UpgradeCanisterArg,
    ) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("upgrade_canister", args).await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }

    #[uniffi::method]
    pub async fn upgrade_specific_individual_canister(&self, arg0: Principal) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("upgrade_specific_individual_canister", args).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn upload_wasms(
        &self,
        arg0: WasmType,
        arg1: serde_bytes::ByteBuf,
    ) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("upload_wasms", args).await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }
}
