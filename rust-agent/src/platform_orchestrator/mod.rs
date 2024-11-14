// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
mod platform_orchestrator_ffi;
use candid::{self, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::Agent;
use std::sync::Arc;
use crate::RUNTIME;
use crate::individual_user_template;

type Result<T> = std::result::Result<T, ic_agent::AgentError>;

#[derive(CandidType, Deserialize)]
pub struct PlatformOrchestratorInitArgs {
    pub version: String,
}
#[derive(CandidType, Deserialize)]
pub enum PlatformOrchestratorResult_ {
    Ok(String),
    Err(String),
}
#[derive(CandidType, Deserialize)]
pub enum WasmType {
    IndividualUserWasm,
    PostCacheWasm,
    SubnetOrchestratorWasm,
}
#[derive(CandidType, Deserialize)]
pub struct UpgradeCanisterArg {
    pub version: String,
    pub canister: WasmType,
    pub wasm_blob: serde_bytes::ByteBuf,
}
#[derive(CandidType, Deserialize)]
pub struct CanisterUpgradeStatus {
    pub failures: Vec<(Principal, String)>,
    pub count: u64,
    pub upgrade_arg: UpgradeCanisterArg,
}
#[derive(CandidType, Deserialize)]
pub struct HttpRequest {
    pub url: String,
    pub method: String,
    pub body: serde_bytes::ByteBuf,
    pub headers: Vec<(String, String)>,
}
#[derive(CandidType, Deserialize)]
pub struct HttpResponse {
    pub body: serde_bytes::ByteBuf,
    pub headers: Vec<(String, String)>,
    pub status_code: u16,
}
#[derive(CandidType, Deserialize)]
pub enum PlatformOrchestratorResult1 {
    Ok(Principal),
    Err(String),
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
    pub async fn deposit_cycles_to_canister(
        &self,
        arg0: Principal,
        arg1: candid::Nat,
    ) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self
            .agent
            .update(&self.principal, "deposit_cycles_to_canister")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }
    pub async fn get_all_available_subnet_orchestrators(&self) -> Result<Vec<Principal>> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .query(&self.principal, "get_all_available_subnet_orchestrators")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, Vec<Principal>)?)
    }
    pub async fn get_all_subnet_orchestrators(&self) -> Result<Vec<Principal>> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .query(&self.principal, "get_all_subnet_orchestrators")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, Vec<Principal>)?)
    }
    pub async fn get_subnet_last_upgrade_status(&self) -> Result<CanisterUpgradeStatus> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .query(&self.principal, "get_subnet_last_upgrade_status")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, CanisterUpgradeStatus)?)
    }
    pub async fn get_version(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .query(&self.principal, "get_version")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, String)?)
    }
    pub async fn http_request(&self, arg0: HttpRequest) -> Result<HttpResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .query(&self.principal, "http_request")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, HttpResponse)?)
    }
    pub async fn provision_subnet_orchestrator_canister(&self, arg0: Principal) -> Result<PlatformOrchestratorResult1> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "provision_subnet_orchestrator_canister")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult1)?)
    }
    pub async fn start_reclaiming_cycles_from_individual_canisters(&self) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .update(
                &self.principal,
                "start_reclaiming_cycles_from_individual_canisters",
            )
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }
    pub async fn start_reclaiming_cycles_from_subnet_orchestrator_canister(
        &self,
    ) -> Result<String> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .update(
                &self.principal,
                "start_reclaiming_cycles_from_subnet_orchestrator_canister",
            )
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, String)?)
    }
    pub async fn stop_upgrades_for_individual_user_canisters(&self) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .update(
                &self.principal,
                "stop_upgrades_for_individual_user_canisters",
            )
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }
    pub async fn subnet_orchestrator_maxed_out(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .update(&self.principal, "subnet_orchestrator_maxed_out")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes)?)
    }
    pub async fn update_profile_owner_for_individual_canisters(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .update(
                &self.principal,
                "update_profile_owner_for_individual_canisters",
            )
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes)?)
    }
    pub async fn upgrade_canister(&self, arg0: UpgradeCanisterArg) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "upgrade_canister")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }
    pub async fn upgrade_specific_individual_canister(&self, arg0: Principal) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "upgrade_specific_individual_canister")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes)?)
    }
    pub async fn upload_wasms(
        &self,
        arg0: WasmType,
        arg1: serde_bytes::ByteBuf,
    ) -> Result<PlatformOrchestratorResult_> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self
            .agent
            .update(&self.principal, "upload_wasms")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, PlatformOrchestratorResult_)?)
    }
}