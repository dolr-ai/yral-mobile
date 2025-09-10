use crate::sns_swap::{Principals};
use candid::{CandidType, Deserialize, Nat, Principal};
use serde_bytes::ByteBuf;
use uniffi::{Enum, Record};
use crate::uni_ffi_helpers::KeyValuePair;

pub type BlockIndex = Nat;
pub type Timestamp = u64;
pub type Tokens = Nat;
pub type SubAccount = ByteBuf;
pub type TxId = Nat;
pub type TxIndex = Nat;

#[derive(CandidType, Deserialize, Enum)]
pub enum CanisterStatusType {
    #[serde(rename = "stopped")]
    Stopped,
    #[serde(rename = "stopping")]
    Stopping,
    #[serde(rename = "running")]
    Running,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum KnownPrincipalType {
    CanisterIdUserIndex,
    CanisterIdPlatformOrchestrator,
    CanisterIdConfiguration,
    CanisterIdHotOrNotSubnetOrchestrator,
    CanisterIdProjectMemberIndex,
    CanisterIdTopicCacheIndex,
    CanisterIdRootCanister,
    CanisterIdDataBackup,
    CanisterIdSnsWasm,
    CanisterIdPostCache,
    #[serde(rename = "CanisterIdSNSController")]
    CanisterIdSnsController,
    CanisterIdSnsGovernance,
    UserIdGlobalSuperAdmin,
}
#[derive(CandidType, Deserialize, Record)]
pub struct KnownPrincipalTypePrincipalPair {
    pub known_principal_type: KnownPrincipalType,
    pub principal: Principal,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum PostStatus {
    BannedForExplicitness,
    BannedDueToUserReporting,
    Uploaded,
    CheckingExplicitness,
    ReadyToView,
    Transcoding,
    Deleted,
}
#[derive(CandidType, Deserialize, Record)]
pub struct CanisterStatusResultV2 {
    pub status: CanisterStatusType,
    pub memory_size: Nat,
    pub cycles: Nat,
    pub settings: DefiniteCanisterSettingsArgs,
    pub idle_cycles_burned_per_day: Nat,
    pub module_hash: Option<ByteBuf>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct LinearScalingCoefficient {
    pub slope_numerator: Option<u64>,
    pub intercept_icp_e8s: Option<u64>,
    pub from_direct_participation_icp_e8s: Option<u64>,
    pub slope_denominator: Option<u64>,
    pub to_direct_participation_icp_e8s: Option<u64>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum TransferError {
    GenericError {
        message: String,
        error_code: Nat,
    },
    TemporarilyUnavailable,
    BadBurn {
        min_burn_amount: Tokens,
    },
    Duplicate {
        duplicate_of: BlockIndex,
    },
    BadFee {
        expected_fee: Tokens,
    },
    CreatedInFuture {
        ledger_time: Timestamp,
    },
    TooOld,
    InsufficientFunds {
        balance: Tokens,
    },
}

#[derive(CandidType, Deserialize, Record)]
pub struct Account {
    pub owner: Principal,
    pub subaccount: Option<ByteBuf>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Burn {
    pub from: Account,
    pub memo: Option<ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: Nat,
    pub spender: Option<Account>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Approve {
    pub fee: Option<Nat>,
    pub from: Account,
    pub memo: Option<ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: Nat,
    pub expected_allowance: Option<Nat>,
    pub expires_at: Option<u64>,
    pub spender: Account,
}

#[derive(CandidType, Deserialize, Record)]
pub struct CanisterCallError {
    pub code: Option<i32>,
    pub description: String,
}

#[derive(CandidType, Deserialize, Record)]
pub struct CfNeuron {
    pub has_created_neuron_recipes: Option<bool>,
    pub hotkeys: Option<Principals>,
    pub nns_neuron_id: u64,
    pub amount_icp_e8s: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct CfParticipant {
    pub controller: Option<Principal>,
    pub hotkey_principal: String,
    pub cf_neurons: Vec<CfNeuron>,
}


#[derive(CandidType, Deserialize, Record)]
pub struct Countries {
    pub iso_codes: Vec<String>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DefiniteCanisterSettingsArgs {
    pub freezing_threshold: Nat,
    pub controllers: Vec<Principal>,
    pub wasm_memory_limit: Option<Nat>,
    pub memory_allocation: Nat,
    pub compute_allocation: Nat,
}

#[derive(CandidType, Deserialize, Record)]
pub struct FailedUpdate {
    pub err: Option<CanisterCallError>,
    pub dapp_canister_id: Option<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GovernanceError {
    pub error_message: String,
    pub error_type: i32,
}

#[derive(CandidType, Deserialize, Record)]
pub struct HttpRequest {
    pub url: String,
    pub method: String,
    pub body: ByteBuf,
    pub headers: Vec<KeyValuePair>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct HttpResponse {
    pub body: ByteBuf,
    pub headers: Vec<KeyValuePair>,
    pub status_code: u16,
}


#[derive(CandidType, Deserialize, Record)]
pub struct IdealMatchedParticipationFunction {
    pub serialized_representation: Option<String>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Mint {
    pub to: Account,
    pub memo: Option<ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: Nat,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronBasketConstructionParameters {
    pub dissolve_delay_interval_seconds: u64,
    pub count: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronId {
    pub id: ByteBuf,
}

#[derive(CandidType, Deserialize, Record)]
pub struct NeuronsFundParticipationConstraints {
    pub coefficient_intervals: Vec<LinearScalingCoefficient>,
    pub max_neurons_fund_participation_icp_e8s: Option<u64>,
    pub min_direct_participation_threshold_icp_e8s: Option<u64>,
    pub ideal_matched_participation_function: Option<IdealMatchedParticipationFunction>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SetDappControllersResponse {
    pub failed_updates: Vec<FailedUpdate>,
}


#[derive(CandidType, Deserialize, Record)]
pub struct SystemTime {
    pub nanos_since_epoch: u32,
    pub secs_since_epoch: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Transaction {
    pub burn: Option<Burn>,
    pub kind: String,
    pub mint: Option<Mint>,
    pub approve: Option<Approve>,
    pub timestamp: u64,
    pub transfer: Option<Transfer>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Transfer {
    pub to: Account,
    pub fee: Option<Nat>,
    pub from: Account,
    pub memo: Option<ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: Nat,
    pub spender: Option<Account>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Subaccount {
    pub subaccount: ByteBuf,
}


