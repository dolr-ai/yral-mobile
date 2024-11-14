use crate::sns_root::*;
use ic_agent::AgentError;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type SnsRootCanister;
        type CanisterIdRecord;
        #[swift_bridge(already_declared)]
        type CanisterStatusType;
        type DefiniteCanisterSettings;
        type CanisterStatusResult;
        type CanisterInstallMode;
        type ChangeCanisterRequest;
        type GetSnsCanistersSummaryRequest;
        #[swift_bridge(already_declared)]
        type DefiniteCanisterSettingsArgs;
        #[swift_bridge(already_declared)]
        type CanisterStatusResultV2;
        type CanisterSummary;
        type GetSnsCanistersSummaryResponse;
        type ListSnsCanistersArg;
        type ListSnsCanistersResponse;
        type ManageDappCanisterSettingsRequest;
        type ManageDappCanisterSettingsResponse;
        type RegisterDappCanisterRequest;
        type RegisterDappCanisterRet;
        type RegisterDappCanistersRequest;
        type RegisterDappCanistersRet;
        type SetDappControllersRequest;
        type CanisterCallError;
        type FailedUpdate;
        type SetDappControllersResponse;
        #[swift_bridge(already_declared)]
        type AgentError;
    }
    extern "Rust" {
        #[swift_bridge(already_declared)]
        type Service;
        async fn canister_status(&self, arg0: CanisterIdRecord) -> Result<CanisterStatusResult, AgentError>;
        async fn change_canister(&self, arg0: ChangeCanisterRequest) -> Result<(), AgentError>;
        async fn get_sns_canisters_summary(&self, arg0: GetSnsCanistersSummaryRequest) -> Result<GetSnsCanistersSummaryResponse, AgentError>;
        async fn list_sns_canisters(&self, arg0: ListSnsCanistersArg) -> Result<ListSnsCanistersResponse, AgentError>;
        async fn manage_dapp_canister_settings(&self, arg0: ManageDappCanisterSettingsRequest) -> Result<ManageDappCanisterSettingsResponse, AgentError>;
        async fn register_dapp_canister(&self, arg0: RegisterDappCanisterRequest) -> Result<RegisterDappCanisterRet, AgentError>;
        async fn register_dapp_canisters(&self, arg0: RegisterDappCanistersRequest) -> Result<RegisterDappCanistersRet, AgentError>;
        async fn set_dapp_controllers(&self, arg0: SetDappControllersRequest) -> Result<SetDappControllersResponse, AgentError>;

    }
}