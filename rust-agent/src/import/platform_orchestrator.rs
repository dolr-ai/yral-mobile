// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
use candid::{self, CandidType, Deserialize, Principal, Encode, Decode};
type Result<T> = std::result::Result<T, ic_agent::AgentError>;

#[derive(CandidType, Deserialize)]
pub struct PlatformOrchestratorInitArgs { pub version: String }
#[derive(CandidType, Deserialize)]
pub enum Result_ { Ok(String), Err(String) }
#[derive(CandidType, Deserialize)]
pub enum WasmType { IndividualUserWasm, PostCacheWasm, SubnetOrchestratorWasm }
#[derive(CandidType, Deserialize)]
pub struct UpgradeCanisterArg {
  pub version: String,
  pub canister: WasmType,
  pub wasm_blob: serde_bytes::ByteBuf,
}
#[derive(CandidType, Deserialize)]
pub struct CanisterUpgradeStatus {
  pub failures: Vec<(Principal,String,)>,
  pub count: u64,
  pub upgrade_arg: UpgradeCanisterArg,
}
#[derive(CandidType, Deserialize)]
pub struct HttpRequest {
  pub url: String,
  pub method: String,
  pub body: serde_bytes::ByteBuf,
  pub headers: Vec<(String,String,)>,
}
#[derive(CandidType, Deserialize)]
pub struct HttpResponse {
  pub body: serde_bytes::ByteBuf,
  pub headers: Vec<(String,String,)>,
  pub status_code: u16,
}
#[derive(CandidType, Deserialize)]
pub enum Result1 { Ok(Principal), Err(String) }

pub struct Service<'a>(pub Principal, pub &'a ic_agent::Agent);
impl<'a> Service<'a> {
  pub async fn deposit_cycles_to_canister(&self, arg0: Principal, arg1: candid::Nat) -> Result<Result_> {
    let args = Encode!(&arg0,&arg1)?;
    let bytes = self.1.update(&self.0, "deposit_cycles_to_canister").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, Result_)?)
  }
  pub async fn get_all_available_subnet_orchestrators(&self) -> Result<Vec<Principal>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "get_all_available_subnet_orchestrators").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Vec<Principal>)?)
  }
  pub async fn get_all_subnet_orchestrators(&self) -> Result<Vec<Principal>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "get_all_subnet_orchestrators").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Vec<Principal>)?)
  }
  pub async fn get_subnet_last_upgrade_status(&self) -> Result<CanisterUpgradeStatus> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "get_subnet_last_upgrade_status").with_arg(args).call().await?;
    Ok(Decode!(&bytes, CanisterUpgradeStatus)?)
  }
  pub async fn get_version(&self) -> Result<String> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "get_version").with_arg(args).call().await?;
    Ok(Decode!(&bytes, String)?)
  }
  pub async fn http_request(&self, arg0: HttpRequest) -> Result<HttpResponse> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.query(&self.0, "http_request").with_arg(args).call().await?;
    Ok(Decode!(&bytes, HttpResponse)?)
  }
  pub async fn provision_subnet_orchestrator_canister(&self, arg0: Principal) -> Result<Result1> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.update(&self.0, "provision_subnet_orchestrator_canister").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, Result1)?)
  }
  pub async fn start_reclaiming_cycles_from_individual_canisters(&self) -> Result<Result_> {
    let args = Encode!()?;
    let bytes = self.1.update(&self.0, "start_reclaiming_cycles_from_individual_canisters").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, Result_)?)
  }
  pub async fn start_reclaiming_cycles_from_subnet_orchestrator_canister(&self) -> Result<String> {
    let args = Encode!()?;
    let bytes = self.1.update(&self.0, "start_reclaiming_cycles_from_subnet_orchestrator_canister").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, String)?)
  }
  pub async fn stop_upgrades_for_individual_user_canisters(&self) -> Result<Result_> {
    let args = Encode!()?;
    let bytes = self.1.update(&self.0, "stop_upgrades_for_individual_user_canisters").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, Result_)?)
  }
  pub async fn subnet_orchestrator_maxed_out(&self) -> Result<()> {
    let args = Encode!()?;
    let bytes = self.1.update(&self.0, "subnet_orchestrator_maxed_out").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes)?)
  }
  pub async fn update_profile_owner_for_individual_canisters(&self) -> Result<()> {
    let args = Encode!()?;
    let bytes = self.1.update(&self.0, "update_profile_owner_for_individual_canisters").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes)?)
  }
  pub async fn upgrade_canister(&self, arg0: UpgradeCanisterArg) -> Result<Result_> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.update(&self.0, "upgrade_canister").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, Result_)?)
  }
  pub async fn upgrade_specific_individual_canister(&self, arg0: Principal) -> Result<()> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.update(&self.0, "upgrade_specific_individual_canister").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes)?)
  }
  pub async fn upload_wasms(&self, arg0: WasmType, arg1: serde_bytes::ByteBuf) -> Result<Result_> {
    let args = Encode!(&arg0,&arg1)?;
    let bytes = self.1.update(&self.0, "upload_wasms").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, Result_)?)
  }
}

