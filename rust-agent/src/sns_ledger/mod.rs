// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
use candid::{self, CandidType, Deserialize, Principal, Encode, Decode};
type Result<T> = std::result::Result<T, ic_agent::AgentError>;

#[derive(CandidType, Deserialize)]
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
  Int(candid::Int),
  Nat(candid::Nat),
  Blob(serde_bytes::ByteBuf),
  Text(String),
}
pub type Subaccount = serde_bytes::ByteBuf;
#[derive(CandidType, Deserialize)]
pub struct Account { pub owner: Principal, pub subaccount: Option<Subaccount> }
#[derive(CandidType, Deserialize)]
pub enum ChangeFeeCollector { SetTo(Account), Unset }
#[derive(CandidType, Deserialize)]
pub struct FeatureFlags { pub icrc2: bool }
#[derive(CandidType, Deserialize)]
pub struct UpgradeArgs {
  pub change_archive_options: Option<ChangeArchiveOptions>,
  pub token_symbol: Option<String>,
  pub transfer_fee: Option<candid::Nat>,
  pub metadata: Option<Vec<(String,MetadataValue,)>>,
  pub maximum_number_of_accounts: Option<u64>,
  pub accounts_overflow_trim_quantity: Option<u64>,
  pub change_fee_collector: Option<ChangeFeeCollector>,
  pub max_memo_length: Option<u16>,
  pub token_name: Option<String>,
  pub feature_flags: Option<FeatureFlags>,
}
#[derive(CandidType, Deserialize)]
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
#[derive(CandidType, Deserialize)]
pub struct InitArgs {
  pub decimals: Option<u8>,
  pub token_symbol: String,
  pub transfer_fee: candid::Nat,
  pub metadata: Vec<(String,MetadataValue,)>,
  pub minting_account: Account,
  pub initial_balances: Vec<(Account,candid::Nat,)>,
  pub maximum_number_of_accounts: Option<u64>,
  pub accounts_overflow_trim_quantity: Option<u64>,
  pub fee_collector_account: Option<Account>,
  pub archive_options: InitArgsArchiveOptions,
  pub max_memo_length: Option<u16>,
  pub token_name: String,
  pub feature_flags: Option<FeatureFlags>,
}
#[derive(CandidType, Deserialize)]
pub enum LedgerArg { Upgrade(Option<UpgradeArgs>), Init(InitArgs) }
pub type BlockIndex = candid::Nat;
#[derive(CandidType, Deserialize)]
pub struct ArchiveInfo {
  pub block_range_end: BlockIndex,
  pub canister_id: Principal,
  pub block_range_start: BlockIndex,
}
#[derive(CandidType, Deserialize)]
pub struct GetBlocksArgs { pub start: BlockIndex, pub length: candid::Nat }
pub type Map = Vec<(String,Box<Value>,)>;
#[derive(CandidType, Deserialize)]
pub enum Value {
  Int(candid::Int),
  Map(Map),
  Nat(candid::Nat),
  Nat64(u64),
  Blob(serde_bytes::ByteBuf),
  Text(String),
  Array(Vec<Box<Value>>),
}
pub type Block = Box<Value>;
#[derive(CandidType, Deserialize)]
pub struct BlockRange { pub blocks: Vec<Block> }
candid::define_function!(pub QueryBlockArchiveFn : (GetBlocksArgs) -> (
    BlockRange,
  ) query);
#[derive(CandidType, Deserialize)]
pub struct GetBlocksResponseArchivedBlocksItem {
  pub callback: QueryBlockArchiveFn,
  pub start: BlockIndex,
  pub length: candid::Nat,
}
#[derive(CandidType, Deserialize)]
pub struct GetBlocksResponse {
  pub certificate: Option<serde_bytes::ByteBuf>,
  pub first_index: BlockIndex,
  pub blocks: Vec<Block>,
  pub chain_length: u64,
  pub archived_blocks: Vec<GetBlocksResponseArchivedBlocksItem>,
}
#[derive(CandidType, Deserialize)]
pub struct DataCertificate {
  pub certificate: Option<serde_bytes::ByteBuf>,
  pub hash_tree: serde_bytes::ByteBuf,
}
pub type TxIndex = candid::Nat;
#[derive(CandidType, Deserialize)]
pub struct GetTransactionsRequest {
  pub start: TxIndex,
  pub length: candid::Nat,
}
pub type Timestamp = u64;
#[derive(CandidType, Deserialize)]
pub struct Burn {
  pub from: Account,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Timestamp>,
  pub amount: candid::Nat,
  pub spender: Option<Account>,
}
#[derive(CandidType, Deserialize)]
pub struct Mint {
  pub to: Account,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Timestamp>,
  pub amount: candid::Nat,
}
#[derive(CandidType, Deserialize)]
pub struct Approve {
  pub fee: Option<candid::Nat>,
  pub from: Account,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Timestamp>,
  pub amount: candid::Nat,
  pub expected_allowance: Option<candid::Nat>,
  pub expires_at: Option<Timestamp>,
  pub spender: Account,
}
#[derive(CandidType, Deserialize)]
pub struct Transfer {
  pub to: Account,
  pub fee: Option<candid::Nat>,
  pub from: Account,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Timestamp>,
  pub amount: candid::Nat,
  pub spender: Option<Account>,
}
#[derive(CandidType, Deserialize)]
pub struct Transaction {
  pub burn: Option<Burn>,
  pub kind: String,
  pub mint: Option<Mint>,
  pub approve: Option<Approve>,
  pub timestamp: Timestamp,
  pub transfer: Option<Transfer>,
}
#[derive(CandidType, Deserialize)]
pub struct TransactionRange { pub transactions: Vec<Transaction> }
candid::define_function!(pub QueryArchiveFn : (GetTransactionsRequest) -> (
    TransactionRange,
  ) query);
#[derive(CandidType, Deserialize)]
pub struct GetTransactionsResponseArchivedTransactionsItem {
  pub callback: QueryArchiveFn,
  pub start: TxIndex,
  pub length: candid::Nat,
}
#[derive(CandidType, Deserialize)]
pub struct GetTransactionsResponse {
  pub first_index: TxIndex,
  pub log_length: candid::Nat,
  pub transactions: Vec<Transaction>,
  pub archived_transactions: Vec<
    GetTransactionsResponseArchivedTransactionsItem
  >,
}
pub type Tokens = candid::Nat;
#[derive(CandidType, Deserialize)]
pub struct StandardRecord { pub url: String, pub name: String }
#[derive(CandidType, Deserialize)]
pub struct TransferArg {
  pub to: Account,
  pub fee: Option<Tokens>,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub from_subaccount: Option<Subaccount>,
  pub created_at_time: Option<Timestamp>,
  pub amount: Tokens,
}
#[derive(CandidType, Deserialize)]
pub enum TransferError {
  GenericError{ message: String, error_code: candid::Nat },
  TemporarilyUnavailable,
  BadBurn{ min_burn_amount: Tokens },
  Duplicate{ duplicate_of: BlockIndex },
  BadFee{ expected_fee: Tokens },
  CreatedInFuture{ ledger_time: Timestamp },
  TooOld,
  InsufficientFunds{ balance: Tokens },
}
#[derive(CandidType, Deserialize)]
pub enum TransferResult { Ok(BlockIndex), Err(TransferError) }
#[derive(CandidType, Deserialize)]
pub struct AllowanceArgs { pub account: Account, pub spender: Account }
#[derive(CandidType, Deserialize)]
pub struct Allowance {
  pub allowance: candid::Nat,
  pub expires_at: Option<Timestamp>,
}
#[derive(CandidType, Deserialize)]
pub struct ApproveArgs {
  pub fee: Option<candid::Nat>,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub from_subaccount: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Timestamp>,
  pub amount: candid::Nat,
  pub expected_allowance: Option<candid::Nat>,
  pub expires_at: Option<Timestamp>,
  pub spender: Account,
}
#[derive(CandidType, Deserialize)]
pub enum ApproveError {
  GenericError{ message: String, error_code: candid::Nat },
  TemporarilyUnavailable,
  Duplicate{ duplicate_of: BlockIndex },
  BadFee{ expected_fee: candid::Nat },
  AllowanceChanged{ current_allowance: candid::Nat },
  CreatedInFuture{ ledger_time: Timestamp },
  TooOld,
  Expired{ ledger_time: Timestamp },
  InsufficientFunds{ balance: candid::Nat },
}
#[derive(CandidType, Deserialize)]
pub enum ApproveResult { Ok(BlockIndex), Err(ApproveError) }
#[derive(CandidType, Deserialize)]
pub struct TransferFromArgs {
  pub to: Account,
  pub fee: Option<Tokens>,
  pub spender_subaccount: Option<Subaccount>,
  pub from: Account,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Timestamp>,
  pub amount: Tokens,
}
#[derive(CandidType, Deserialize)]
pub enum TransferFromError {
  GenericError{ message: String, error_code: candid::Nat },
  TemporarilyUnavailable,
  InsufficientAllowance{ allowance: Tokens },
  BadBurn{ min_burn_amount: Tokens },
  Duplicate{ duplicate_of: BlockIndex },
  BadFee{ expected_fee: Tokens },
  CreatedInFuture{ ledger_time: Timestamp },
  TooOld,
  InsufficientFunds{ balance: Tokens },
}
#[derive(CandidType, Deserialize)]
pub enum TransferFromResult { Ok(BlockIndex), Err(TransferFromError) }
#[derive(CandidType, Deserialize)]
pub struct GetArchivesArgs { pub from: Option<Principal> }
#[derive(CandidType, Deserialize)]
pub struct GetArchivesResultItem {
  pub end: candid::Nat,
  pub canister_id: Principal,
  pub start: candid::Nat,
}
pub type GetArchivesResult = Vec<GetArchivesResultItem>;
#[derive(CandidType, Deserialize)]
pub enum Icrc3Value {
  Int(candid::Int),
  Map(Vec<(String,Box<Icrc3Value>,)>),
  Nat(candid::Nat),
  Blob(serde_bytes::ByteBuf),
  Text(String),
  Array(Vec<Box<Icrc3Value>>),
}
#[derive(CandidType, Deserialize)]
pub struct GetBlocksResultBlocksItem {
  pub id: candid::Nat,
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
  pub log_length: candid::Nat,
  pub blocks: Vec<GetBlocksResultBlocksItem>,
  pub archived_blocks: Vec<GetBlocksResultArchivedBlocksItem>,
}
#[derive(CandidType, Deserialize)]
pub struct Icrc3DataCertificate {
  pub certificate: serde_bytes::ByteBuf,
  pub hash_tree: serde_bytes::ByteBuf,
}
#[derive(CandidType, Deserialize)]
pub struct Icrc3SupportedBlockTypesRetItem {
  pub url: String,
  pub block_type: String,
}

pub struct Service<'a>(pub Principal, pub &'a ic_agent::Agent);
impl<'a> Service<'a> {
  pub async fn archives(&self) -> Result<Vec<ArchiveInfo>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "archives").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Vec<ArchiveInfo>)?)
  }
  pub async fn get_blocks(&self, arg0: GetBlocksArgs) -> Result<GetBlocksResponse> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.query(&self.0, "get_blocks").with_arg(args).call().await?;
    Ok(Decode!(&bytes, GetBlocksResponse)?)
  }
  pub async fn get_data_certificate(&self) -> Result<DataCertificate> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "get_data_certificate").with_arg(args).call().await?;
    Ok(Decode!(&bytes, DataCertificate)?)
  }
  pub async fn get_transactions(&self, arg0: GetTransactionsRequest) -> Result<GetTransactionsResponse> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.query(&self.0, "get_transactions").with_arg(args).call().await?;
    Ok(Decode!(&bytes, GetTransactionsResponse)?)
  }
  pub async fn icrc_1_balance_of(&self, arg0: Account) -> Result<Tokens> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.query(&self.0, "icrc1_balance_of").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Tokens)?)
  }
  pub async fn icrc_1_decimals(&self) -> Result<u8> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_decimals").with_arg(args).call().await?;
    Ok(Decode!(&bytes, u8)?)
  }
  pub async fn icrc_1_fee(&self) -> Result<Tokens> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_fee").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Tokens)?)
  }
  pub async fn icrc_1_metadata(&self) -> Result<Vec<(String,MetadataValue,)>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_metadata").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Vec<(String,MetadataValue,)>)?)
  }
  pub async fn icrc_1_minting_account(&self) -> Result<Option<Account>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_minting_account").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Option<Account>)?)
  }
  pub async fn icrc_1_name(&self) -> Result<String> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_name").with_arg(args).call().await?;
    Ok(Decode!(&bytes, String)?)
  }
  pub async fn icrc_1_supported_standards(&self) -> Result<Vec<StandardRecord>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_supported_standards").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Vec<StandardRecord>)?)
  }
  pub async fn icrc_1_symbol(&self) -> Result<String> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_symbol").with_arg(args).call().await?;
    Ok(Decode!(&bytes, String)?)
  }
  pub async fn icrc_1_total_supply(&self) -> Result<Tokens> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc1_total_supply").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Tokens)?)
  }
  pub async fn icrc_1_transfer(&self, arg0: TransferArg) -> Result<TransferResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.update(&self.0, "icrc1_transfer").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, TransferResult)?)
  }
  pub async fn icrc_2_allowance(&self, arg0: AllowanceArgs) -> Result<Allowance> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.query(&self.0, "icrc2_allowance").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Allowance)?)
  }
  pub async fn icrc_2_approve(&self, arg0: ApproveArgs) -> Result<ApproveResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.update(&self.0, "icrc2_approve").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, ApproveResult)?)
  }
  pub async fn icrc_2_transfer_from(&self, arg0: TransferFromArgs) -> Result<TransferFromResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.update(&self.0, "icrc2_transfer_from").with_arg(args).call_and_wait().await?;
    Ok(Decode!(&bytes, TransferFromResult)?)
  }
  pub async fn icrc_3_get_archives(&self, arg0: GetArchivesArgs) -> Result<GetArchivesResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.query(&self.0, "icrc3_get_archives").with_arg(args).call().await?;
    Ok(Decode!(&bytes, GetArchivesResult)?)
  }
  pub async fn icrc_3_get_blocks(&self, arg0: Vec<GetBlocksArgs>) -> Result<GetBlocksResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.1.query(&self.0, "icrc3_get_blocks").with_arg(args).call().await?;
    Ok(Decode!(&bytes, GetBlocksResult)?)
  }
  pub async fn icrc_3_get_tip_certificate(&self) -> Result<Option<Icrc3DataCertificate>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc3_get_tip_certificate").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Option<Icrc3DataCertificate>)?)
  }
  pub async fn icrc_3_supported_block_types(&self) -> Result<Vec<Icrc3SupportedBlockTypesRetItem>> {
    let args = Encode!()?;
    let bytes = self.1.query(&self.0, "icrc3_supported_block_types").with_arg(args).call().await?;
    Ok(Decode!(&bytes, Vec<Icrc3SupportedBlockTypesRetItem>)?)
  }
}

