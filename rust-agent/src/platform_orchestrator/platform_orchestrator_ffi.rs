use crate::platform_orchestrator::*;
use ic_agent::export::PrincipalError;
use ic_agent::AgentError;
use serde_bytes::ByteBuf;
use candid::Nat;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type PlatformOrchestratorInitArgs;
        type PlatformOrchestratorResult_;
        type WasmType;
        type UpgradeCanisterArg;
        type CanisterUpgradeStatus;
        #[swift_bridge(already_declared)]
        type HttpRequest;
        #[swift_bridge(already_declared)]
        type HttpResponse;
        type PlatformOrchestratorResult1;
        #[swift_bridge(already_declared)]
        type Principal;
        #[swift_bridge(already_declared)]
        type PrincipalError;
        #[swift_bridge(already_declared)]
        type AgentError;
        #[swift_bridge(already_declared)]
        type ByteBuf;
        #[swift_bridge(already_declared)]
        type Nat;
    }

    extern  "Rust" {
        #[swift_bridge(already_declared)]
        type Service;
        async fn deposit_cycles_to_canister(
            &self,
            arg0: Principal,
            arg1: Nat,
        ) -> Result<PlatformOrchestratorResult_, AgentError>;

        async fn get_all_available_subnet_orchestrators(
            &self,
        ) -> Result<Vec<Principal>, AgentError>;

        async fn get_all_subnet_orchestrators(
            &self,
        ) -> Result<Vec<Principal>, AgentError>;

        async fn get_subnet_last_upgrade_status(
            &self,
        ) -> Result<CanisterUpgradeStatus, AgentError>;

        async fn provision_subnet_orchestrator_canister(
            &self,
            arg0: Principal,
        ) -> Result<PlatformOrchestratorResult1, AgentError>;

        async fn start_reclaiming_cycles_from_individual_canisters(
            &self,
        ) -> Result<PlatformOrchestratorResult_, AgentError>;

        async fn start_reclaiming_cycles_from_subnet_orchestrator_canister(
            &self,
        ) -> Result<String, AgentError>;

        async fn stop_upgrades_for_individual_user_canisters(
            &self,
        ) -> Result<PlatformOrchestratorResult_, AgentError>;

        async fn subnet_orchestrator_maxed_out(
            &self,
        ) -> Result<(), AgentError>;

        async fn update_profile_owner_for_individual_canisters(
            &self,
        ) -> Result<(), AgentError>;

        async fn upgrade_canister(
            &self,
            arg0: UpgradeCanisterArg,
        ) -> Result<PlatformOrchestratorResult_, AgentError>;

        async fn upgrade_specific_individual_canister(
            &self,
            arg0: Principal,
        ) -> Result<(), AgentError>;

        async fn upload_wasms(
            &self,
            arg0: WasmType,
            arg1: ByteBuf,
        ) -> Result<PlatformOrchestratorResult_, AgentError>;
    }
}
