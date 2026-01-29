// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
use candid::{self, CandidType, Decode, Deserialize, Encode, Int, Nat, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::Agent;
use std::sync::Arc;
use uniffi::{Record, Enum};
use crate::uni_ffi_helpers::*;
use crate::commons::*;
use crate::individual_user_template;
use crate::RUNTIME;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct ChangeArchiveOptions {
    pub num_blocks_to_archive: Option<u64>,
    pub max_transactions_per_response: Option<u64>,
    pub trigger_threshold: Option<u64>,
    pub more_controller_ids: Option<Vec<Principal>>,
    pub max_message_size_bytes: Option<u64>,
    pub cycles_for_archive_creation: Option<u64>,
    pub node_max_memory_size_bytes: Option<u64>,
    pub controller_id: Option<Principal>,
}

#[derive(CandidType, Deserialize)]
pub enum MetadataValue {
    Int(Int),
    Nat(Nat),
    Blob(serde_bytes::ByteBuf),
    Text(String),
}

#[derive(CandidType, Deserialize, Enum)]
pub enum AccountResult {
    Found(Account),
    NotFound,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ChangeFeeCollector {
    SetTo(Account),
    Unset,
}

#[derive(CandidType, Deserialize, Record)]
pub struct FeatureFlags {
    pub icrc2: bool,
}

#[derive(CandidType, Deserialize)]
pub struct StringMetaDataPair {
    pub string: String,
    pub metadata: MetadataValue,
}

#[derive(CandidType, Deserialize)]
pub struct UpgradeArgs {
    pub change_archive_options: Option<ChangeArchiveOptions>,
    pub token_symbol: Option<String>,
    pub transfer_fee: Option<Nat>,
    pub metadata: Option<Vec<StringMetaDataPair>>,
    pub maximum_number_of_accounts: Option<u64>,
    pub accounts_overflow_trim_quantity: Option<u64>,
    pub change_fee_collector: Option<ChangeFeeCollector>,
    pub max_memo_length: Option<u16>,
    pub token_name: Option<String>,
    pub feature_flags: Option<FeatureFlags>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct InitArgsArchiveOptions {
    pub num_blocks_to_archive: u64,
    pub max_transactions_per_response: Option<u64>,
    pub trigger_threshold: u64,
    pub more_controller_ids: Option<Vec<Principal>>,
    pub max_message_size_bytes: Option<u64>,
    pub cycles_for_archive_creation: Option<u64>,
    pub node_max_memory_size_bytes: Option<u64>,
    pub controller_id: Principal,
}

#[derive(CandidType, Deserialize, Record)]
pub struct AccountNatPair {
    pub account: Account,
    pub nat: Nat,
}

#[derive(CandidType, Deserialize)]
pub struct InitArgs {
    pub decimals: Option<u8>,
    pub token_symbol: String,
    pub transfer_fee: Nat,
    pub metadata: Vec<StringMetaDataPair>,
    pub minting_account: Account,
    pub initial_balances: Vec<AccountNatPair>,
    pub maximum_number_of_accounts: Option<u64>,
    pub accounts_overflow_trim_quantity: Option<u64>,
    pub fee_collector_account: Option<Account>,
    pub archive_options: InitArgsArchiveOptions,
    pub max_memo_length: Option<u16>,
    pub token_name: String,
    pub feature_flags: Option<FeatureFlags>,
}

#[derive(CandidType, Deserialize)]
pub enum LedgerArg {
    Upgrade(Option<UpgradeArgs>),
    Init(InitArgs),
}

#[derive(CandidType, Deserialize, Record)]
pub struct ArchiveInfo {
    pub block_range_end: BlockIndex,
    pub canister_id: Principal,
    pub block_range_start: BlockIndex,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetBlocksArgs {
    pub start: BlockIndex,
    pub length: Nat,
}

pub type Map = Vec<(String, Box<Value>)>;

#[derive(CandidType, Deserialize)]
pub enum Value {
    Int(Int),
    Map(Map),
    Nat(Nat),
    Nat64(u64),
    Blob(serde_bytes::ByteBuf),
    Text(String),
    Array(Vec<Box<Value>>),
}

pub type Block = Box<Value>;

#[derive(CandidType, Deserialize)]
pub struct BlockRange {
    pub blocks: Vec<Block>,
}

candid::define_function!(pub QueryBlockArchiveFn : (GetBlocksArgs) -> (BlockRange,) query);

#[derive(CandidType, Deserialize)]
pub struct GetBlocksResponseArchivedBlocksItem {
    pub callback: QueryBlockArchiveFn,
    pub start: BlockIndex,
    pub length: Nat,
}

#[derive(CandidType, Deserialize)]
pub struct GetBlocksResponse {
    pub certificate: Option<serde_bytes::ByteBuf>,
    pub first_index: BlockIndex,
    pub blocks: Vec<Block>,
    pub chain_length: u64,
    pub archived_blocks: Vec<GetBlocksResponseArchivedBlocksItem>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct DataCertificate {
    pub certificate: Option<serde_bytes::ByteBuf>,
    pub hash_tree: serde_bytes::ByteBuf,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetTransactionsRequest {
    pub start: TxIndex,
    pub length: Nat,
}

#[derive(CandidType, Deserialize, Record)]
pub struct TransactionRange {
    pub transactions: Vec<Transaction>,
}

candid::define_function!(pub QueryArchiveFn : (GetTransactionsRequest) -> (TransactionRange,) query);

#[derive(CandidType, Deserialize)]
pub struct GetTransactionsResponseArchivedTransactionsItem {
    pub callback: QueryArchiveFn,
    pub start: TxIndex,
    pub length: Nat,
}

#[derive(CandidType, Deserialize)]
pub struct GetTransactionsResponse {
    pub first_index: TxIndex,
    pub log_length: Nat,
    pub transactions: Vec<Transaction>,
    pub archived_transactions: Vec<GetTransactionsResponseArchivedTransactionsItem>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct StandardRecord {
    pub url: String,
    pub name: String,
}

#[derive(CandidType, Deserialize, Record)]
pub struct TransferArg {
    pub to: Account,
    pub fee: Option<Tokens>,
    pub memo: Option<serde_bytes::ByteBuf>,
    pub from_subaccount: Option<SubAccount>,
    pub created_at_time: Option<Timestamp>,
    pub amount: Tokens,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum TransferResult {
    Ok(BlockIndex),
    Err(TransferError),
}

#[derive(CandidType, Deserialize, Record)]
pub struct AllowanceArgs {
    pub account: Account,
    pub spender: Account,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Allowance {
    pub allowance: Nat,
    pub expires_at: Option<Timestamp>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ApproveArgs {
    pub fee: Option<Nat>,
    pub memo: Option<serde_bytes::ByteBuf>,
    pub from_subaccount: Option<serde_bytes::ByteBuf>,
    pub created_at_time: Option<Timestamp>,
    pub amount: Nat,
    pub expected_allowance: Option<Nat>,
    pub expires_at: Option<Timestamp>,
    pub spender: Account,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ApproveError {
    GenericError {
        message: String,
        error_code: Nat,
    },
    TemporarilyUnavailable,
    Duplicate {
        duplicate_of: BlockIndex,
    },
    BadFee {
        expected_fee: Nat,
    },
    AllowanceChanged {
        current_allowance: Nat,
    },
    CreatedInFuture {
        ledger_time: Timestamp,
    },
    TooOld,
    Expired {
        ledger_time: Timestamp,
    },
    InsufficientFunds {
        balance: Nat,
    },
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ApproveResult {
    Ok(BlockIndex),
    Err(ApproveError),
}

#[derive(CandidType, Deserialize, Record)]
pub struct TransferFromArgs {
    pub to: Account,
    pub fee: Option<Tokens>,
    pub spender_subaccount: Option<SubAccount>,
    pub from: Account,
    pub memo: Option<serde_bytes::ByteBuf>,
    pub created_at_time: Option<Timestamp>,
    pub amount: Tokens,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum TransferFromError {
    GenericError {
        message: String,
        error_code: Nat,
    },
    TemporarilyUnavailable,
    InsufficientAllowance {
        allowance: Tokens,
    },
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

#[derive(CandidType, Deserialize, Enum)]
pub enum TransferFromResult {
    Ok(BlockIndex),
    Err(TransferFromError),
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetArchivesArgs {
    pub from: Option<Principal>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetArchivesResultItem {
    pub end: Nat,
    pub canister_id: Principal,
    pub start: Nat,
}

pub type GetArchivesResult = Vec<GetArchivesResultItem>;

#[derive(CandidType, Deserialize)]
pub enum Icrc3Value {
    Int(Int),
    Map(Vec<(String, Box<Icrc3Value>)>),
    Nat(Nat),
    Blob(serde_bytes::ByteBuf),
    Text(String),
    Array(Vec<Box<Icrc3Value>>),
}

#[derive(CandidType, Deserialize)]
pub struct GetBlocksResultBlocksItem {
    pub id: Nat,
    pub block: Box<Icrc3Value>,
}

candid::define_function!(pub GetBlocksResultArchivedBlocksItemCallback : (
    Vec<GetBlocksArgs>,
  ) -> (GetBlocksResult) query);

#[derive(CandidType, Deserialize)]
pub struct GetBlocksResultArchivedBlocksItem {
    pub args: Vec<GetBlocksArgs>,
    pub callback: GetBlocksResultArchivedBlocksItemCallback,
}

#[derive(CandidType, Deserialize)]
pub struct GetBlocksResult {
    pub log_length: Nat,
    pub blocks: Vec<GetBlocksResultBlocksItem>,
    pub archived_blocks: Vec<GetBlocksResultArchivedBlocksItem>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Icrc3DataCertificate {
    pub certificate: serde_bytes::ByteBuf,
    pub hash_tree: serde_bytes::ByteBuf,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Icrc3DataCertificateResult {
    Found(Icrc3DataCertificate),
    NotFound,
}

#[derive(CandidType, Deserialize, Record)]
pub struct Icrc3SupportedBlockTypesRetItem {
    pub url: String,
    pub block_type: String,
}

#[derive(CandidType, Deserialize)]
pub struct MetadataEntry {
    pub key: String,
    pub value: MetadataValue,
}

#[derive(uniffi::Object)]
pub struct SnsLedgerService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

pub const DEFAULT_SNS_LEDGER_CANISTER: &str = "6rdgd-kyaaa-aaaaq-aaavq-cai";

#[uniffi::export]
impl SnsLedgerService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<SnsLedgerService, FFIError> {
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
        let principal = Principal::from_text(DEFAULT_SNS_LEDGER_CANISTER)
          .map_err(|e| FFIError::PrincipalError(format!("Invalid default principal: {:?}", e)))?;
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
    pub async fn archives(&self) -> Result<Vec<ArchiveInfo>> {
        let args = Encode!()?;
        let bytes = self.query_canister("archives", args).await?;
        Ok(Decode!(&bytes, Vec<ArchiveInfo>)?)
    }

    // TODO: callbacks not supported in uniffi need to figure this out
    // pub async fn get_blocks(&self, arg0: GetBlocksArgs) -> Result<GetBlocksResponse> {
    //     let args = Encode!(&arg0)?;
    //     let bytes = self.query_canister("get_blocks", args).await?;
    //     Ok(Decode!(&bytes, GetBlocksResponse)?)
    // }

    #[uniffi::method]
    pub async fn get_data_certificate(&self) -> Result<DataCertificate> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_data_certificate", args).await?;
        Ok(Decode!(&bytes, DataCertificate)?)
    }

    // TODO: callbacks not supported in uniffi need to figure this out
    // pub async fn get_transactions(
    //     &self,
    //     arg0: GetTransactionsRequest,
    // ) -> Result<GetTransactionsResponse> {
    //     let args = Encode!(&arg0)?;
    //     let bytes = self.query_canister("get_transactions", args).await?;
    //     Ok(Decode!(&bytes, GetTransactionsResponse)?)
    // }

    #[uniffi::method]
    pub async fn icrc_1_balance_of(&self, arg0: Account) -> Result<Tokens> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("icrc1_balance_of", args).await?;
        Ok(Decode!(&bytes, Tokens)?)
    }

    #[uniffi::method]
    pub async fn icrc_1_decimals(&self) -> Result<u8> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc1_decimals", args).await?;
        Ok(Decode!(&bytes, u8)?)
    }

    #[uniffi::method]
    pub async fn icrc_1_fee(&self) -> Result<Tokens> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc1_fee", args).await?;
        Ok(Decode!(&bytes, Tokens)?)
    }
    
    // TODO: candid::int not supported in uniffi need to figure this out
    // pub async fn icrc_1_metadata(&self) -> Result<Vec<MetadataEntry>> {
    //     let args = Encode!()?;
    //     let bytes = self.query_canister("icrc1_metadata", args).await?;
    //     Ok(Decode!(&bytes, Vec<MetadataEntry>)?)
    // }

    #[uniffi::method]
    pub async fn icrc_1_minting_account(&self) -> Result<AccountResult> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc1_minting_account", args).await?;
        Ok(Decode!(&bytes, AccountResult)?)
    }

    #[uniffi::method]
    pub async fn icrc_1_name(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc1_name", args).await?;
        Ok(Decode!(&bytes, String)?)
    }

    #[uniffi::method]
    pub async fn icrc_1_supported_standards(&self) -> Result<Vec<StandardRecord>> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc1_supported_standards", args).await?;
        Ok(Decode!(&bytes, Vec<StandardRecord>)?)
    }

    #[uniffi::method]
    pub async fn icrc_1_symbol(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc1_symbol", args).await?;
        Ok(Decode!(&bytes, String)?)
    }

    #[uniffi::method]
    pub async fn icrc_1_total_supply(&self) -> Result<Tokens> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc1_total_supply", args).await?;
        Ok(Decode!(&bytes, Tokens)?)
    }

    #[uniffi::method]
    pub async fn icrc_1_transfer(&self, arg0: TransferArg) -> Result<TransferResult> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("icrc1_transfer", args).await?;
        Ok(Decode!(&bytes, TransferResult)?)
    }

    #[uniffi::method]
    pub async fn icrc_2_allowance(&self, arg0: AllowanceArgs) -> Result<Allowance> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("icrc2_allowance", args).await?;
        Ok(Decode!(&bytes, Allowance)?)
    }

    #[uniffi::method]
    pub async fn icrc_2_approve(&self, arg0: ApproveArgs) -> Result<ApproveResult> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("icrc2_approve", args).await?;
        Ok(Decode!(&bytes, ApproveResult)?)
    }

    #[uniffi::method]
    pub async fn icrc_2_transfer_from(&self, arg0: TransferFromArgs) -> Result<TransferFromResult> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("icrc2_transfer_from", args).await?;
        Ok(Decode!(&bytes, TransferFromResult)?)
    }

    #[uniffi::method]
    pub async fn icrc_3_get_archives(&self, arg0: GetArchivesArgs) -> Result<GetArchivesResult> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("icrc3_get_archives", args).await?;
        Ok(Decode!(&bytes, GetArchivesResult)?)
    }

    // TODO: callbacks not supported in uniffi need to figure this out
    // pub async fn icrc_3_get_blocks(&self, arg0: Vec<GetBlocksArgs>) -> Result<GetBlocksResult> {
    //     let args = Encode!(&arg0)?;
    //     let bytes = self.query_canister("icrc3_get_blocks", args).await?;
    //     Ok(Decode!(&bytes, GetBlocksResult)?)
    // }

    #[uniffi::method]
    pub async fn icrc_3_get_tip_certificate(&self) -> Result<Icrc3DataCertificateResult> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc3_get_tip_certificate", args).await?;
        Ok(Decode!(&bytes, Icrc3DataCertificateResult)?)
    }

    #[uniffi::method]
    pub async fn icrc_3_supported_block_types(
        &self,
    ) -> Result<Vec<Icrc3SupportedBlockTypesRetItem>> {
        let args = Encode!()?;
        let bytes = self.query_canister("icrc3_supported_block_types", args).await?;
        Ok(Decode!(&bytes, Vec<Icrc3SupportedBlockTypesRetItem>)?)
    }
}
