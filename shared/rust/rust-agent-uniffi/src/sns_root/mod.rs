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
pub struct SnsRootCanister {
    pub dapp_canister_ids: Vec<Principal>,
    pub testflight: bool,
    pub latest_ledger_archive_poll_timestamp_seconds: Option<u64>,
    pub archive_canister_ids: Vec<Principal>,
    pub governance_canister_id: Option<Principal>,
    pub index_canister_id: Option<Principal>,
    pub swap_canister_id: Option<Principal>,
    pub ledger_canister_id: Option<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct CanisterIdRecord {
    pub canister_id: Principal,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DefiniteCanisterSettings {
    pub freezing_threshold: Option<candid::Nat>,
    pub controllers: Vec<Principal>,
    pub reserved_cycles_limit: Option<candid::Nat>,
    pub memory_allocation: Option<candid::Nat>,
    pub compute_allocation: Option<candid::Nat>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct CanisterStatusResult {
    pub status: CanisterStatusType,
    pub memory_size: candid::Nat,
    pub cycles: candid::Nat,
    pub settings: DefiniteCanisterSettings,
    pub idle_cycles_burned_per_day: Option<candid::Nat>,
    pub module_hash: Option<serde_bytes::ByteBuf>,
    pub reserved_cycles: Option<candid::Nat>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum CanisterInstallMode {
    #[serde(rename = "reinstall")]
    Reinstall,
    #[serde(rename = "upgrade")]
    Upgrade,
    #[serde(rename = "install")]
    Install,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ChangeCanisterRequest {
    pub arg: serde_bytes::ByteBuf,
    pub wasm_module: serde_bytes::ByteBuf,
    pub stop_before_installing: bool,
    pub mode: CanisterInstallMode,
    pub canister_id: Principal,
    pub memory_allocation: Option<candid::Nat>,
    pub compute_allocation: Option<candid::Nat>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetSnsCanistersSummaryRequest {
    pub update_canister_list: Option<bool>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct CanisterSummary {
    pub status: Option<CanisterStatusResultV2>,
    pub canister_id: Option<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetSnsCanistersSummaryResponse {
    pub root: Option<CanisterSummary>,
    pub swap: Option<CanisterSummary>,
    pub ledger: Option<CanisterSummary>,
    pub index: Option<CanisterSummary>,
    pub governance: Option<CanisterSummary>,
    pub dapps: Vec<CanisterSummary>,
    pub archives: Vec<CanisterSummary>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ListSnsCanistersArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct ListSnsCanistersResponse {
    pub root: Option<Principal>,
    pub swap: Option<Principal>,
    pub ledger: Option<Principal>,
    pub index: Option<Principal>,
    pub governance: Option<Principal>,
    pub dapps: Vec<Principal>,
    pub archives: Vec<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ManageDappCanisterSettingsRequest {
    pub freezing_threshold: Option<u64>,
    pub canister_ids: Vec<Principal>,
    pub reserved_cycles_limit: Option<u64>,
    pub log_visibility: Option<i32>,
    pub memory_allocation: Option<u64>,
    pub compute_allocation: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ManageDappCanisterSettingsResponse {
    pub failure_reason: Option<String>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RegisterDappCanisterRequest {
    pub canister_id: Option<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RegisterDappCanisterRet {}

#[derive(CandidType, Deserialize, Record)]
pub struct RegisterDappCanistersRequest {
    pub canister_ids: Vec<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RegisterDappCanistersRet {}

#[derive(CandidType, Deserialize, Record)]
pub struct SetDappControllersRequest {
    pub canister_ids: Option<RegisterDappCanistersRequest>,
    pub controller_principal_ids: Vec<Principal>,
}

#[derive(uniffi::Object)]
pub struct SnsRootService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl SnsRootService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<SnsRootService, FFIError> {
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
    pub async fn canister_status(&self, arg0: CanisterIdRecord) -> Result<CanisterStatusResult> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("canister_status", args).await?;
        Ok(Decode!(&bytes, CanisterStatusResult)?)
    }

    #[uniffi::method]
    pub async fn change_canister(&self, arg0: ChangeCanisterRequest) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("change_canister", args).await?;
        Ok(Decode!(&bytes)?)
    }

    #[uniffi::method]
    pub async fn get_build_metadata(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_build_metadata", args).await?;
        Ok(Decode!(&bytes, String)?)
    }

    #[uniffi::method]
    pub async fn get_sns_canisters_summary(
        &self,
        arg0: GetSnsCanistersSummaryRequest,
    ) -> Result<GetSnsCanistersSummaryResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("get_sns_canisters_summary", args).await?;
        Ok(Decode!(&bytes, GetSnsCanistersSummaryResponse)?)
    }

    #[uniffi::method]
    pub async fn list_sns_canisters(
        &self,
        arg0: ListSnsCanistersArg,
    ) -> Result<ListSnsCanistersResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("list_sns_canisters", args).await?;
        Ok(Decode!(&bytes, ListSnsCanistersResponse)?)
    }

    #[uniffi::method]
    pub async fn manage_dapp_canister_settings(
        &self,
        arg0: ManageDappCanisterSettingsRequest,
    ) -> Result<ManageDappCanisterSettingsResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("manage_dapp_canister_settings", args).await?;
        Ok(Decode!(&bytes, ManageDappCanisterSettingsResponse)?)
    }

    #[uniffi::method]
    pub async fn register_dapp_canister(
        &self,
        arg0: RegisterDappCanisterRequest,
    ) -> Result<RegisterDappCanisterRet> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("register_dapp_canister", args).await?;
        Ok(Decode!(&bytes, RegisterDappCanisterRet)?)
    }

    #[uniffi::method]
    pub async fn register_dapp_canisters(
        &self,
        arg0: RegisterDappCanistersRequest,
    ) -> Result<RegisterDappCanistersRet> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("register_dapp_canisters", args).await?;
        Ok(Decode!(&bytes, RegisterDappCanistersRet)?)
    }

    #[uniffi::method]
    pub async fn set_dapp_controllers(
        &self,
        arg0: SetDappControllersRequest,
    ) -> Result<SetDappControllersResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("set_dapp_controllers", args).await?;
        Ok(Decode!(&bytes, SetDappControllersResponse)?)
    }
}
