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
use crate::commons::{CanisterStatusResultV2, CanisterStatusType, GovernanceError, NeuronId, Subaccount};

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct GenericNervousSystemFunctionParams {
    pub validator_canister_id: Option<Principal>,
    pub target_canister_id: Option<Principal>,
    pub validator_method_name: Option<String>,
    pub target_method_name: Option<String>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum FunctionType {
    NativeNervousSystemFunction {},
    GenericNervousSystemFunction(GenericNervousSystemFunctionParams),
}

#[derive(CandidType, Deserialize, Record)]
pub struct NervousSystemFunction {
    pub id: u64,
    pub name: String,
    pub description: Option<String>,
    pub function_type: Option<FunctionType>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GovernanceCachedMetrics {
    pub not_dissolving_neurons_e8s_buckets: Vec<IntDoublePair>,
    pub garbage_collectable_neurons_count: u64,
    pub neurons_with_invalid_stake_count: u64,
    pub not_dissolving_neurons_count_buckets: Vec<IntPair>,
    pub neurons_with_less_than_6_months_dissolve_delay_count: u64,
    pub dissolved_neurons_count: u64,
    pub total_staked_e8s: u64,
    pub total_supply_governance_tokens: u64,
    pub not_dissolving_neurons_count: u64,
    pub dissolved_neurons_e8s: u64,
    pub neurons_with_less_than_6_months_dissolve_delay_e8s: u64,
    pub dissolving_neurons_count_buckets: Vec<IntPair>,
    pub dissolving_neurons_count: u64,
    pub dissolving_neurons_e8s_buckets: Vec<IntDoublePair>,
    pub timestamp_seconds: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct MaturityModulation {
    pub current_basis_points: Option<i32>,
    pub updated_at_timestamp_seconds: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Followees {
    pub followees: Vec<NeuronId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct IntFolloweesPair {
    pub first: u64,
    pub second: Followees,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DefaultFollowees {
    pub followees: Vec<IntFolloweesPair>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronPermissionList {
    pub permissions: Vec<i32>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct VotingRewardsParameters {
    pub final_reward_rate_basis_points: Option<u64>,
    pub initial_reward_rate_basis_points: Option<u64>,
    pub reward_rate_transition_duration_seconds: Option<u64>,
    pub round_duration_seconds: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NervousSystemParameters {
    pub default_followees: Option<DefaultFollowees>,
    pub max_dissolve_delay_seconds: Option<u64>,
    pub max_dissolve_delay_bonus_percentage: Option<u64>,
    pub max_followees_per_function: Option<u64>,
    pub neuron_claimer_permissions: Option<NeuronPermissionList>,
    pub neuron_minimum_stake_e8s: Option<u64>,
    pub max_neuron_age_for_age_bonus: Option<u64>,
    pub initial_voting_period_seconds: Option<u64>,
    pub neuron_minimum_dissolve_delay_to_vote_seconds: Option<u64>,
    pub reject_cost_e8s: Option<u64>,
    pub max_proposals_to_keep_per_action: Option<u32>,
    pub wait_for_quiet_deadline_increase_seconds: Option<u64>,
    pub max_number_of_neurons: Option<u64>,
    pub transaction_fee_e8s: Option<u64>,
    pub max_number_of_proposals_with_ballots: Option<u64>,
    pub max_age_bonus_percentage: Option<u64>,
    pub neuron_grantable_permissions: Option<NeuronPermissionList>,
    pub voting_rewards_parameters: Option<VotingRewardsParameters>,
    pub maturity_modulation_disabled: Option<bool>,
    pub max_number_of_principals_per_neuron: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Version {
    pub archive_wasm_hash: serde_bytes::ByteBuf,
    pub root_wasm_hash: serde_bytes::ByteBuf,
    pub swap_wasm_hash: serde_bytes::ByteBuf,
    pub ledger_wasm_hash: serde_bytes::ByteBuf,
    pub governance_wasm_hash: serde_bytes::ByteBuf,
    pub index_wasm_hash: serde_bytes::ByteBuf,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ProposalId {
    pub id: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RewardEvent {
    pub rounds_since_last_distribution: Option<u64>,
    pub actual_timestamp_seconds: u64,
    pub end_timestamp_seconds: Option<u64>,
    pub total_available_e8s_equivalent: Option<u64>,
    pub distributed_e8s_equivalent: u64,
    pub round: u64,
    pub settled_proposals: Vec<ProposalId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct UpgradeInProgress {
    pub mark_failed_at_seconds: u64,
    pub checking_upgrade_lock: u64,
    pub proposal_id: u64,
    pub target_version: Option<Version>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GovernanceAccount {
    pub owner: Option<Principal>,
    pub subaccount: Option<Subaccount>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Decimal {
    pub human_readable: Option<String>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Tokens {
    pub e8s: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ValuationFactors {
    pub xdrs_per_icp: Option<Decimal>,
    pub icps_per_token: Option<Decimal>,
    pub tokens: Option<Tokens>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Valuation {
    pub token: Option<i32>,
    pub account: Option<GovernanceAccount>,
    pub valuation_factors: Option<ValuationFactors>,
    pub timestamp_seconds: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct MintSnsTokensActionAuxiliary {
    pub valuation: Option<Valuation>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ActionAuxiliary {
    TransferSnsTreasuryFunds(MintSnsTokensActionAuxiliary),
    MintSnsTokens(MintSnsTokensActionAuxiliary),
}

#[derive(CandidType, Deserialize, Record)]
pub struct Ballot {
    pub vote: i32,
    pub cast_timestamp_seconds: u64,
    pub voting_power: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Percentage {
    pub basis_points: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Tally {
    pub no: u64,
    pub yes: u64,
    pub total: u64,
    pub timestamp_seconds: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ManageDappCanisterSettingsParams {
    pub freezing_threshold: Option<u64>,
    pub canister_ids: Vec<Principal>,
    pub reserved_cycles_limit: Option<u64>,
    pub log_visibility: Option<i32>,
    pub memory_allocation: Option<u64>,
    pub compute_allocation: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RegisterDappCanistersParams {
    pub canister_ids: Vec<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct TransferSnsTreasuryFundsParams {
    pub from_treasury: i32,
    pub to_principal: Option<Principal>,
    pub to_subaccount: Option<Subaccount>,
    pub memo: Option<u64>,
    pub amount_e8s: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct UpgradeSnsControlledCanisterParams {
    pub new_canister_wasm: serde_bytes::ByteBuf,
    pub mode: Option<i32>,
    pub canister_id: Option<Principal>,
    pub canister_upgrade_arg: Option<serde_bytes::ByteBuf>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DeregisterDappCanistersParams {
    pub canister_ids: Vec<Principal>,
    pub new_controllers: Vec<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct MintSnsTokensParams {
    pub to_principal: Option<Principal>,
    pub to_subaccount: Option<Subaccount>,
    pub memo: Option<u64>,
    pub amount_e8s: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ManageSnsMetadataParams {
    pub url: Option<String>,
    pub logo: Option<String>,
    pub name: Option<String>,
    pub description: Option<String>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ExecuteGenericNervousSystemFunctionParams {
    pub function_id: u64,
    pub payload: serde_bytes::ByteBuf,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ManageLedgerParams {
    pub transfer_fee: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct MotionParams {
    pub motion_text: String,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Action {
    ManageNervousSystemParameters(NervousSystemParameters),
    AddGenericNervousSystemFunction(NervousSystemFunction),
    ManageDappCanisterSettings(ManageDappCanisterSettingsParams),
    RemoveGenericNervousSystemFunction(u64),
    UpgradeSnsToNextVersion {},
    RegisterDappCanisters(RegisterDappCanistersParams),
    TransferSnsTreasuryFunds(TransferSnsTreasuryFundsParams),
    UpgradeSnsControlledCanister(UpgradeSnsControlledCanisterParams),
    DeregisterDappCanisters(DeregisterDappCanistersParams),
    MintSnsTokens(MintSnsTokensParams),
    Unspecified {},
    ManageSnsMetadata(ManageSnsMetadataParams),
    ExecuteGenericNervousSystemFunction(ExecuteGenericNervousSystemFunctionParams),
    ManageLedgerParameters(ManageLedgerParams),
    Motion(MotionParams),
}

#[derive(CandidType, Deserialize, Record)]
pub struct ProposalParams {
    pub url: String,
    pub title: String,
    pub action: Option<Action>,
    pub summary: String,
}

#[derive(CandidType, Deserialize, Record)]
pub struct WaitForQuietState {
    pub current_deadline_timestamp_seconds: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct StringBallotPair {
    pub first: String,
    pub second: Ballot,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ProposalData {
    pub id: Option<ProposalId>,
    pub payload_text_rendering: Option<String>,
    pub action: u64,
    pub failure_reason: Option<GovernanceError>,
    pub action_auxiliary: Option<ActionAuxiliary>,
    pub ballots: Vec<StringBallotPair>,
    pub minimum_yes_proportion_of_total: Option<Percentage>,
    pub reward_event_round: u64,
    pub failed_timestamp_seconds: u64,
    pub reward_event_end_timestamp_seconds: Option<u64>,
    pub proposal_creation_timestamp_seconds: u64,
    pub initial_voting_period_seconds: u64,
    pub reject_cost_e8s: u64,
    pub latest_tally: Option<Tally>,
    pub wait_for_quiet_deadline_increase_seconds: u64,
    pub decided_timestamp_seconds: u64,
    pub proposal: Option<ProposalParams>,
    pub proposer: Option<NeuronId>,
    pub wait_for_quiet_state: Option<WaitForQuietState>,
    pub minimum_yes_proportion_of_exercised: Option<Percentage>,
    pub is_eligible_for_rewards: bool,
    pub executed_timestamp_seconds: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SplitParams {
    pub memo: u64,
    pub amount_e8s: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct FollowParams {
    pub function_id: u64,
    pub followees: Vec<NeuronId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DisburseMaturityParams {
    pub to_account: Option<GovernanceAccount>,
    pub percentage_to_disburse: u32,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ChangeAutoStakeMaturityParams {
    pub requested_setting_for_auto_stake_maturity: bool,
}

#[derive(CandidType, Deserialize, Record)]
pub struct IncreaseDissolveDelayParams {
    pub additional_dissolve_delay_seconds: u32,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SetDissolveTimestampParams {
    pub dissolve_timestamp_seconds: u64,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Operation {
    ChangeAutoStakeMaturity(ChangeAutoStakeMaturityParams),
    StopDissolving {},
    StartDissolving {},
    IncreaseDissolveDelay(IncreaseDissolveDelayParams),
    SetDissolveTimestamp(SetDissolveTimestampParams),
}

#[derive(CandidType, Deserialize, Record)]
pub struct ConfigureParams {
    pub operation: Option<Operation>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RegisterVoteParams {
    pub vote: i32,
    pub proposal: Option<ProposalId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct FinalizeDisburseMaturityParams {
    pub amount_to_be_disbursed_e8s: u64,
    pub to_account: Option<GovernanceAccount>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct MemoAndControllerParams {
    pub controller: Option<Principal>,
    pub memo: u64,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum By {
    MemoAndController(MemoAndControllerParams),
    NeuronId {},
}

#[derive(CandidType, Deserialize, Record)]
pub struct ClaimOrRefreshParams {
    pub by: Option<By>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct RemoveNeuronPermissionsParams {
    pub permissions_to_remove: Option<NeuronPermissionList>,
    pub principal_id: Option<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct AddNeuronPermissionsParams {
    pub permissions_to_add: Option<NeuronPermissionList>,
    pub principal_id: Option<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct MergeMaturityParams {
    pub percentage_to_merge: u32,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Amount {
    pub e8s: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DisburseParams {
    pub to_account: Option<GovernanceAccount>,
    pub amount: Option<Amount>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Command2 {
    Split(SplitParams),
    Follow(FollowParams),
    DisburseMaturity(DisburseMaturityParams),
    Configure(ConfigureParams),
    RegisterVote(RegisterVoteParams),
    SyncCommand {},
    MakeProposal(ProposalParams),
    FinalizeDisburseMaturity(FinalizeDisburseMaturityParams),
    ClaimOrRefreshNeuron(ClaimOrRefreshParams),
    RemoveNeuronPermissions(RemoveNeuronPermissionsParams),
    AddNeuronPermissions(AddNeuronPermissionsParams),
    MergeMaturity(MergeMaturityParams),
    Disburse(DisburseParams),
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronInFlightCommand {
    pub command: Option<Command2>,
    pub timestamp: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronPermission {
    pub principal: Option<Principal>,
    pub permission_type: Vec<i32>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum DissolveState {
    DissolveDelaySeconds(u64),
    WhenDissolvedTimestampSeconds(u64),
}

#[derive(CandidType, Deserialize, Record)]
pub struct DisburseMaturityInProgress {
    pub timestamp_of_disbursement_seconds: u64,
    pub amount_e8s: u64,
    pub account_to_disburse_to: Option<GovernanceAccount>,
    pub finalize_disbursement_timestamp_seconds: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronParams {
    pub id: Option<NeuronId>,
    pub staked_maturity_e8s_equivalent: Option<u64>,
    pub permissions: Vec<NeuronPermission>,
    pub maturity_e8s_equivalent: u64,
    pub cached_neuron_stake_e8s: u64,
    pub created_timestamp_seconds: u64,
    pub source_nns_neuron_id: Option<u64>,
    pub auto_stake_maturity: Option<bool>,
    pub aging_since_timestamp_seconds: u64,
    pub dissolve_state: Option<DissolveState>,
    pub voting_power_percentage_multiplier: u64,
    pub vesting_period_seconds: Option<u64>,
    pub disburse_maturity_in_progress: Vec<DisburseMaturityInProgress>,
    pub followees: Vec<IntFolloweesPair>,
    pub neuron_fees_e8s: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct IntNervousSystemFunctionPair {
    pub first: u64,
    pub second: NervousSystemFunction,
}

#[derive(CandidType, Deserialize, Record)]
pub struct StringNeuronInFlightCommandPair {
    pub first: String,
    pub second: NeuronInFlightCommand,
}

#[derive(CandidType, Deserialize, Record)]
pub struct IntProposalDataPair {
    pub first: u64,
    pub second: ProposalData,
}

#[derive(CandidType, Deserialize, Record)]
pub struct StringNeuronPair {
    pub first: String,
    pub second: NeuronParams,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Governance {
    pub root_canister_id: Option<Principal>,
    pub id_to_nervous_system_functions: Vec<IntNervousSystemFunctionPair>,
    pub metrics: Option<GovernanceCachedMetrics>,
    pub maturity_modulation: Option<MaturityModulation>,
    pub mode: i32,
    pub parameters: Option<NervousSystemParameters>,
    pub is_finalizing_disburse_maturity: Option<bool>,
    pub deployed_version: Option<Version>,
    pub sns_initialization_parameters: String,
    pub latest_reward_event: Option<RewardEvent>,
    pub pending_version: Option<UpgradeInProgress>,
    pub swap_canister_id: Option<Principal>,
    pub ledger_canister_id: Option<Principal>,
    pub proposals: Vec<IntProposalDataPair>,
    pub in_flight_commands: Vec<StringNeuronInFlightCommandPair>,
    pub sns_metadata: Option<ManageSnsMetadataParams>,
    pub neurons: Vec<StringNeuronPair>,
    pub genesis_timestamp_seconds: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronParameters {
    pub controller: Option<Principal>,
    pub dissolve_delay_seconds: Option<u64>,
    pub source_nns_neuron_id: Option<u64>,
    pub stake_e8s: Option<u64>,
    pub followees: Vec<NeuronId>,
    pub hotkey: Option<Principal>,
    pub neuron_id: Option<NeuronId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ClaimSwapNeuronsRequest {
    pub neuron_parameters: Vec<NeuronParameters>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SwapNeuron {
    pub id: Option<NeuronId>,
    pub status: i32,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ClaimedSwapNeurons {
    pub swap_neurons: Vec<SwapNeuron>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ClaimSwapNeuronsResult {
    Ok(ClaimedSwapNeurons),
    Err(i32),
}

#[derive(CandidType, Deserialize, Record)]
pub struct ClaimSwapNeuronsResponse {
    pub claim_swap_neurons_result: Option<ClaimSwapNeuronsResult>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct FailStuckUpgradeInProgressArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct FailStuckUpgradeInProgressRet {}

#[derive(CandidType, Deserialize, Record)]
pub struct GetMaturityModulationArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct GetMaturityModulationResponse {
    pub maturity_modulation: Option<MaturityModulation>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetMetadataArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct GetMetadataResponse {
    pub url: Option<String>,
    pub logo: Option<String>,
    pub name: Option<String>,
    pub description: Option<String>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetModeArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct GetModeResponse {
    pub mode: Option<i32>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetNeuron {
    pub neuron_id: Option<NeuronId>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum SNSGovernanceResult_ {
    Error(GovernanceError),
    Neuron(NeuronParams),
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetNeuronResponse {
    pub result: Option<SNSGovernanceResult_>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetProposal {
    pub proposal_id: Option<ProposalId>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum SNSGovernanceResult1 {
    Error(GovernanceError),
    Proposal(ProposalData),
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetProposalResponse {
    pub result: Option<SNSGovernanceResult1>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetRunningSnsVersionArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct GetRunningSnsVersionResponse {
    pub deployed_version: Option<Version>,
    pub pending_version: Option<UpgradeInProgress>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetSnsInitializationParametersArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct GetSnsInitializationParametersResponse {
    pub sns_initialization_parameters: String,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ListNervousSystemFunctionsResponse {
    pub reserved_ids: Vec<u64>,
    pub functions: Vec<NervousSystemFunction>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ListNeurons {
    pub of_principal: Option<Principal>,
    pub limit: u32,
    pub start_page_at: Option<NeuronId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ListNeuronsResponse {
    pub neurons: Vec<NeuronParams>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ListProposals {
    pub include_reward_status: Vec<i32>,
    pub before_proposal: Option<ProposalId>,
    pub limit: u32,
    pub exclude_type: Vec<u64>,
    pub include_status: Vec<i32>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ListProposalsResponse {
    pub include_ballots_by_caller: Option<bool>,
    pub proposals: Vec<ProposalData>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct StakeMaturityParams {
    pub percentage_to_stake: Option<u32>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Command {
    Split(SplitParams),
    Follow(FollowParams),
    DisburseMaturity(DisburseMaturityParams),
    ClaimOrRefresh(ClaimOrRefreshParams),
    Configure(ConfigureParams),
    RegisterVote(RegisterVoteParams),
    MakeProposal(ProposalParams),
    StakeMaturity(StakeMaturityParams),
    RemoveNeuronPermissions(RemoveNeuronPermissionsParams),
    AddNeuronPermissions(AddNeuronPermissionsParams),
    MergeMaturity(MergeMaturityParams),
    Disburse(DisburseParams),
}

#[derive(CandidType, Deserialize, Record)]
pub struct ManageNeuron {
    pub subaccount: serde_bytes::ByteBuf,
    pub command: Option<Command>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SplitResponse {
    pub created_neuron_id: Option<NeuronId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DisburseMaturityResponse {
    pub amount_disbursed_e8s: u64,
    pub amount_deducted_e8s: Option<u64>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ClaimOrRefreshResponse {
    pub refreshed_neuron_id: Option<NeuronId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct StakeMaturityResponse {
    pub maturity_e8s: u64,
    pub staked_maturity_e8s: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct MergeMaturityResponse {
    pub merged_maturity_e8s: u64,
    pub new_stake_e8s: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DisburseResponse {
    pub transfer_block_height: u64,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Command1 {
    Error(GovernanceError),
    Split(SplitResponse),
    Follow {},
    DisburseMaturity(DisburseMaturityResponse),
    ClaimOrRefresh(ClaimOrRefreshResponse),
    Configure {},
    RegisterVote {},
    MakeProposal(GetProposal),
    RemoveNeuronPermission {},
    StakeMaturity(StakeMaturityResponse),
    MergeMaturity(MergeMaturityResponse),
    Disburse(DisburseResponse),
    AddNeuronPermission {},
}

#[derive(CandidType, Deserialize, Record)]
pub struct ManageNeuronResponse {
    pub command: Option<Command1>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SetMode {
    pub mode: i32,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SetModeRet {}

#[derive(uniffi::Object)]
pub struct SnsGovernanceService {
    pub principal: Principal,
    agent: Arc<Agent>,
}

#[uniffi::export]
impl SnsGovernanceService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<SnsGovernanceService, FFIError> {
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
    pub async fn claim_swap_neurons(
        &self,
        request: ClaimSwapNeuronsRequest,
    ) -> Result<ClaimSwapNeuronsResponse> {
        let args = Encode!(&request)?;
        let bytes = self.update_canister("claim_swap_neurons", args).await?;
        Ok(Decode!(&bytes, ClaimSwapNeuronsResponse)?)
    }

    #[uniffi::method]
    pub async fn fail_stuck_upgrade_in_progress(
        &self,
        request: FailStuckUpgradeInProgressArg,
    ) -> Result<FailStuckUpgradeInProgressRet> {
        let args = Encode!(&request)?;
        let bytes = self.update_canister("fail_stuck_upgrade_in_progress", args).await?;
        Ok(Decode!(&bytes, FailStuckUpgradeInProgressRet)?)
    }

    #[uniffi::method]
    pub async fn get_build_metadata(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_build_metadata", args).await?;
        Ok(Decode!(&bytes, String)?)
    }

    #[uniffi::method]
    pub async fn get_latest_reward_event(&self) -> Result<RewardEvent> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_latest_reward_event", args).await?;
        Ok(Decode!(&bytes, RewardEvent)?)
    }

    #[uniffi::method]
    pub async fn get_maturity_modulation(
        &self,
        request: GetMaturityModulationArg,
    ) -> Result<GetMaturityModulationResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("get_maturity_modulation", args).await?;
        Ok(Decode!(&bytes, GetMaturityModulationResponse)?)
    }

    #[uniffi::method]
    pub async fn get_metadata(&self, request: GetMetadataArg) -> Result<GetMetadataResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("get_metadata", args).await?;
        Ok(Decode!(&bytes, GetMetadataResponse)?)
    }

    #[uniffi::method]
    pub async fn get_mode(&self, request: GetModeArg) -> Result<GetModeResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("get_mode", args).await?;
        Ok(Decode!(&bytes, GetModeResponse)?)
    }

    #[uniffi::method]
    pub async fn get_nervous_system_parameters(&self) -> Result<NervousSystemParameters> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_nervous_system_parameters", args).await?;
        Ok(Decode!(&bytes, NervousSystemParameters)?)
    }

    #[uniffi::method]
    pub async fn get_neuron(&self, request: GetNeuron) -> Result<GetNeuronResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("get_neuron", args).await?;
        Ok(Decode!(&bytes, GetNeuronResponse)?)
    }

    #[uniffi::method]
    pub async fn get_proposal(&self, request: GetProposal) -> Result<GetProposalResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("get_proposal", args).await?;
        Ok(Decode!(&bytes, GetProposalResponse)?)
    }

    #[uniffi::method]
    pub async fn get_root_canister_status(&self) -> Result<CanisterStatusResultV2> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_root_canister_status", args).await?;
        Ok(Decode!(&bytes, CanisterStatusResultV2)?)
    }

    #[uniffi::method]
    pub async fn get_running_sns_version(
        &self,
        request: GetRunningSnsVersionArg,
    ) -> Result<GetRunningSnsVersionResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("get_running_sns_version", args).await?;
        Ok(Decode!(&bytes, GetRunningSnsVersionResponse)?)
    }

    #[uniffi::method]
    pub async fn get_sns_initialization_parameters(
        &self,
        request: GetSnsInitializationParametersArg,
    ) -> Result<GetSnsInitializationParametersResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("get_sns_initialization_parameters", args).await?;
        Ok(Decode!(&bytes, GetSnsInitializationParametersResponse)?)
    }

    #[uniffi::method]
    pub async fn list_nervous_system_functions(
        &self,
    ) -> Result<ListNervousSystemFunctionsResponse> {
        let args = Encode!()?;
        let bytes = self.query_canister("list_nervous_system_functions", args).await?;
        Ok(Decode!(&bytes, ListNervousSystemFunctionsResponse)?)
    }

    #[uniffi::method]
    pub async fn list_neurons(&self, request: ListNeurons) -> Result<ListNeuronsResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("list_neurons", args).await?;
        Ok(Decode!(&bytes, ListNeuronsResponse)?)
    }

    #[uniffi::method]
    pub async fn list_proposals(&self, request: ListProposals) -> Result<ListProposalsResponse> {
        let args = Encode!(&request)?;
        let bytes = self.query_canister("list_proposals", args).await?;
        Ok(Decode!(&bytes, ListProposalsResponse)?)
    }

    #[uniffi::method]
    pub async fn manage_neuron(&self, request: ManageNeuron) -> Result<ManageNeuronResponse> {
        let args = Encode!(&request)?;
        let bytes = self.update_canister("manage_neuron", args).await?;
        Ok(Decode!(&bytes, ManageNeuronResponse)?)
    }

    #[uniffi::method]
    pub async fn set_mode(&self, request: SetMode) -> Result<SetModeRet> {
        let args = Encode!(&request)?;
        let bytes = self.update_canister("set_mode", args).await?;
        Ok(Decode!(&bytes, SetModeRet)?)
    }
}
