// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
mod sns_root_ffi;
use crate::individual_user_template;
use crate::RUNTIME;
use candid::{self, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::Agent;
use std::sync::Arc;
type Result<T> = std::result::Result<T, ic_agent::AgentError>;

#[derive(CandidType, Deserialize)]
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
#[derive(CandidType, Deserialize)]
pub struct CanisterIdRecord {
    pub canister_id: Principal,
}
#[derive(CandidType, Deserialize)]
pub enum CanisterStatusType {
    #[serde(rename = "stopped")]
    Stopped,
    #[serde(rename = "stopping")]
    Stopping,
    #[serde(rename = "running")]
    Running,
}
#[derive(CandidType, Deserialize)]
pub struct DefiniteCanisterSettings {
    pub freezing_threshold: Option<candid::Nat>,
    pub controllers: Vec<Principal>,
    pub reserved_cycles_limit: Option<candid::Nat>,
    pub memory_allocation: Option<candid::Nat>,
    pub compute_allocation: Option<candid::Nat>,
}
#[derive(CandidType, Deserialize)]
pub struct CanisterStatusResult {
    pub status: CanisterStatusType,
    pub memory_size: candid::Nat,
    pub cycles: candid::Nat,
    pub settings: DefiniteCanisterSettings,
    pub idle_cycles_burned_per_day: Option<candid::Nat>,
    pub module_hash: Option<serde_bytes::ByteBuf>,
    pub reserved_cycles: Option<candid::Nat>,
}
#[derive(CandidType, Deserialize)]
pub enum CanisterInstallMode {
    #[serde(rename = "reinstall")]
    Reinstall,
    #[serde(rename = "upgrade")]
    Upgrade,
    #[serde(rename = "install")]
    Install,
}
#[derive(CandidType, Deserialize)]
pub struct ChangeCanisterRequest {
    pub arg: serde_bytes::ByteBuf,
    pub wasm_module: serde_bytes::ByteBuf,
    pub stop_before_installing: bool,
    pub mode: CanisterInstallMode,
    pub canister_id: Principal,
    pub memory_allocation: Option<candid::Nat>,
    pub compute_allocation: Option<candid::Nat>,
}
#[derive(CandidType, Deserialize)]
pub struct GetSnsCanistersSummaryRequest {
    pub update_canister_list: Option<bool>,
}
#[derive(CandidType, Deserialize)]
pub struct DefiniteCanisterSettingsArgs {
    pub freezing_threshold: candid::Nat,
    pub controllers: Vec<Principal>,
    pub memory_allocation: candid::Nat,
    pub compute_allocation: candid::Nat,
}
#[derive(CandidType, Deserialize)]
pub struct CanisterStatusResultV2 {
    pub status: CanisterStatusType,
    pub memory_size: candid::Nat,
    pub cycles: candid::Nat,
    pub settings: DefiniteCanisterSettingsArgs,
    pub idle_cycles_burned_per_day: candid::Nat,
    pub module_hash: Option<serde_bytes::ByteBuf>,
}
#[derive(CandidType, Deserialize)]
pub struct CanisterSummary {
    pub status: Option<CanisterStatusResultV2>,
    pub canister_id: Option<Principal>,
}
#[derive(CandidType, Deserialize)]
pub struct GetSnsCanistersSummaryResponse {
    pub root: Option<CanisterSummary>,
    pub swap: Option<CanisterSummary>,
    pub ledger: Option<CanisterSummary>,
    pub index: Option<CanisterSummary>,
    pub governance: Option<CanisterSummary>,
    pub dapps: Vec<CanisterSummary>,
    pub archives: Vec<CanisterSummary>,
}
#[derive(CandidType, Deserialize)]
pub struct ListSnsCanistersArg {}
#[derive(CandidType, Deserialize)]
pub struct ListSnsCanistersResponse {
    pub root: Option<Principal>,
    pub swap: Option<Principal>,
    pub ledger: Option<Principal>,
    pub index: Option<Principal>,
    pub governance: Option<Principal>,
    pub dapps: Vec<Principal>,
    pub archives: Vec<Principal>,
}
#[derive(CandidType, Deserialize)]
pub struct ManageDappCanisterSettingsRequest {
    pub freezing_threshold: Option<u64>,
    pub canister_ids: Vec<Principal>,
    pub reserved_cycles_limit: Option<u64>,
    pub log_visibility: Option<i32>,
    pub memory_allocation: Option<u64>,
    pub compute_allocation: Option<u64>,
}
#[derive(CandidType, Deserialize)]
pub struct ManageDappCanisterSettingsResponse {
    pub failure_reason: Option<String>,
}
#[derive(CandidType, Deserialize)]
pub struct RegisterDappCanisterRequest {
    pub canister_id: Option<Principal>,
}
#[derive(CandidType, Deserialize)]
pub struct RegisterDappCanisterRet {}
#[derive(CandidType, Deserialize)]
pub struct RegisterDappCanistersRequest {
    pub canister_ids: Vec<Principal>,
}
#[derive(CandidType, Deserialize)]
pub struct RegisterDappCanistersRet {}
#[derive(CandidType, Deserialize)]
pub struct SetDappControllersRequest {
    pub canister_ids: Option<RegisterDappCanistersRequest>,
    pub controller_principal_ids: Vec<Principal>,
}
#[derive(CandidType, Deserialize)]
pub struct CanisterCallError {
    pub code: Option<i32>,
    pub description: String,
}
#[derive(CandidType, Deserialize)]
pub struct FailedUpdate {
    pub err: Option<CanisterCallError>,
    pub dapp_canister_id: Option<Principal>,
}
#[derive(CandidType, Deserialize)]
pub struct SetDappControllersResponse {
    pub failed_updates: Vec<FailedUpdate>,
}

pub struct Service {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}
impl Service {
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<Service, PrincipalError> {
        let principal = Principal::from_text(principal_text)?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .build()
            .expect("Failed to create agent");
        RUNTIME
            .block_on(agent.fetch_root_key())
            .expect("Failed to fetch root key");
        Ok(Self {
            principal,
            agent: Arc::new(agent),
        })
    }
    pub async fn canister_status(&self, arg0: CanisterIdRecord) -> Result<CanisterStatusResult> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "canister_status")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, CanisterStatusResult)?)
    }
    pub async fn change_canister(&self, arg0: ChangeCanisterRequest) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "change_canister")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes)?)
    }
    pub async fn get_build_metadata(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .query(&self.principal, "get_build_metadata")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, String)?)
    }
    pub async fn get_sns_canisters_summary(
        &self,
        arg0: GetSnsCanistersSummaryRequest,
    ) -> Result<GetSnsCanistersSummaryResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "get_sns_canisters_summary")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, GetSnsCanistersSummaryResponse)?)
    }
    pub async fn list_sns_canisters(
        &self,
        arg0: ListSnsCanistersArg,
    ) -> Result<ListSnsCanistersResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .query(&self.principal, "list_sns_canisters")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, ListSnsCanistersResponse)?)
    }
    pub async fn manage_dapp_canister_settings(
        &self,
        arg0: ManageDappCanisterSettingsRequest,
    ) -> Result<ManageDappCanisterSettingsResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "manage_dapp_canister_settings")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, ManageDappCanisterSettingsResponse)?)
    }
    pub async fn register_dapp_canister(
        &self,
        arg0: RegisterDappCanisterRequest,
    ) -> Result<RegisterDappCanisterRet> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "register_dapp_canister")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, RegisterDappCanisterRet)?)
    }
    pub async fn register_dapp_canisters(
        &self,
        arg0: RegisterDappCanistersRequest,
    ) -> Result<RegisterDappCanistersRet> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "register_dapp_canisters")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, RegisterDappCanistersRet)?)
    }
    pub async fn set_dapp_controllers(
        &self,
        arg0: SetDappControllersRequest,
    ) -> Result<SetDappControllersResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "set_dapp_controllers")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, SetDappControllersResponse)?)
    }
}
