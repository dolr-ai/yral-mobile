use crate::sns_governance::*;
use ic_agent::AgentError;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type GenericNervousSystemFunction;
        type FunctionType;
        type NervousSystemFunction;
        type GovernanceCachedMetrics;
        type MaturityModulation;
        type NeuronId;
        type Followees;
        type DefaultFollowees;
        type NeuronPermissionList;
        type VotingRewardsParameters;
        type NervousSystemParameters;
        type Version;
        type ProposalId;
        type RewardEvent;
        type UpgradeInProgress;
        #[swift_bridge(already_declared)]
        type GovernanceError;
        type Subaccount;
        type Account;
        type Decimal;
        type Tokens;
        type ValuationFactors;
        type Valuation;
        type MintSnsTokensActionAuxiliary;
        type ActionAuxiliary;
        type Ballot;
        type Percentage;
        type Tally;
        type ManageDappCanisterSettings;
        type RegisterDappCanisters;
        type TransferSnsTreasuryFunds;
        type UpgradeSnsControlledCanister;
        type DeregisterDappCanisters;
        type MintSnsTokens;
        type ManageSnsMetadata;
        type ExecuteGenericNervousSystemFunction;
        type ManageLedgerParameters;
        type Motion;
        type Action;
        type Proposal;
        type WaitForQuietState;
        type ProposalData;
        type Split;
        type Follow;
        type DisburseMaturity;
        type ChangeAutoStakeMaturity;
        type IncreaseDissolveDelay;
        type SetDissolveTimestamp;
        type Operation;
        type Configure;
        type RegisterVote;
        type FinalizeDisburseMaturity;
        type MemoAndController;
        type By;
        type ClaimOrRefresh;
        type RemoveNeuronPermissions;
        type AddNeuronPermissions;
        type MergeMaturity;
        type Amount;
        type Disburse;
        type Command2;
        type NeuronInFlightCommand;
        type NeuronPermission;
        type DissolveState;
        type DisburseMaturityInProgress;
        type Neuron;
        type Governance;
        type NeuronParameters;
        type ClaimSwapNeuronsRequest;
        type SwapNeuron;
        type ClaimedSwapNeurons;
        type ClaimSwapNeuronsResult;
        type ClaimSwapNeuronsResponse;
        type FailStuckUpgradeInProgressArg;
        type FailStuckUpgradeInProgressRet;
        type GetMaturityModulationArg;
        type GetMaturityModulationResponse;
        type GetMetadataArg;
        type GetMetadataResponse;
        type GetModeArg;
        type GetModeResponse;
        type GetNeuron;
        type SNSGovernanceResult_;
        type GetNeuronResponse;
        type GetProposal;
        type SNSGovernanceResult1;
        type GetProposalResponse;
        type CanisterStatusType;
        type DefiniteCanisterSettingsArgs;
        type CanisterStatusResultV2;
        type GetRunningSnsVersionArg;
        type GetRunningSnsVersionResponse;
        type GetSnsInitializationParametersArg;
        type GetSnsInitializationParametersResponse;
        type ListNervousSystemFunctionsResponse;
        type ListNeurons;
        type ListNeuronsResponse;
        type ListProposals;
        type ListProposalsResponse;
        type StakeMaturity;
        type Command;
        type ManageNeuron;
        type SplitResponse;
        type DisburseMaturityResponse;
        type ClaimOrRefreshResponse;
        type StakeMaturityResponse;
        type MergeMaturityResponse;
        type DisburseResponse;
        type Command1;
        type ManageNeuronResponse;
        type SetMode;
        type SetModeRet;
        #[swift_bridge(already_declared)]
        type AgentError;
    }

    extern "Rust" {
        #[swift_bridge(already_declared)]
        type Service;
        async fn claim_swap_neurons(
            &self,
            arg0: ClaimSwapNeuronsRequest,
        ) -> Result<ClaimSwapNeuronsResponse, AgentError>;
        async fn fail_stuck_upgrade_in_progress(
            &self,
            arg0: FailStuckUpgradeInProgressArg,
        ) -> Result<FailStuckUpgradeInProgressRet, AgentError>;
        async fn get_build_metadata(&self) -> Result<String, AgentError>;
        async fn get_latest_reward_event(&self) -> Result<RewardEvent, AgentError>;
        async fn get_maturity_modulation(
            &self,
            arg0: GetMaturityModulationArg,
        ) -> Result<GetMaturityModulationResponse, AgentError>;
        async fn get_metadata(
            &self,
            arg0: GetMetadataArg,
        ) -> Result<GetMetadataResponse, AgentError>;
        async fn get_mode(&self, arg0: GetModeArg) -> Result<GetModeResponse, AgentError>;
        async fn get_nervous_system_parameters(
            &self,
            arg0: (),
        ) -> Result<NervousSystemParameters, AgentError>;
        async fn get_neuron(&self, arg0: GetNeuron) -> Result<GetNeuronResponse, AgentError>;
        async fn get_proposal(&self, arg0: GetProposal) -> Result<GetProposalResponse, AgentError>;
        async fn get_root_canister_status(
            &self,
            arg0: (),
        ) -> Result<CanisterStatusResultV2, AgentError>;
        async fn get_running_sns_version(
            &self,
            arg0: GetRunningSnsVersionArg,
        ) -> Result<GetRunningSnsVersionResponse, AgentError>;
        async fn get_sns_initialization_parameters(
            &self,
            arg0: GetSnsInitializationParametersArg,
        ) -> Result<GetSnsInitializationParametersResponse, AgentError>;
        async fn list_nervous_system_functions(
            &self,
        ) -> Result<ListNervousSystemFunctionsResponse, AgentError>;
        async fn list_neurons(&self, arg0: ListNeurons) -> Result<ListNeuronsResponse, AgentError>;
        async fn list_proposals(
            &self,
            arg0: ListProposals,
        ) -> Result<ListProposalsResponse, AgentError>;
        async fn manage_neuron(
            &self,
            arg0: ManageNeuron,
        ) -> Result<ManageNeuronResponse, AgentError>;
        async fn set_mode(&self, arg0: SetMode) -> Result<SetModeRet, AgentError>;
    }
}
