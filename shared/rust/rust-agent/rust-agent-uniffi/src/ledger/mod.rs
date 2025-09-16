// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
use candid::{self, CandidType, Deserialize, Principal, Encode, Decode};
use ic_agent::export::PrincipalError;
use ic_agent::Agent;
use std::sync::Arc;
use uniffi::{Record, Enum};
use crate::uni_ffi_helpers::*;
use crate::commons::*;
use crate::RUNTIME;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct ICPFeatureFlags { pub icrc2: bool }

#[derive(CandidType, Deserialize, Record)]
pub struct UpgradeArgs {
  pub maximum_number_of_accounts: Option<u64>,
  pub icrc1_minting_account: Option<Account>,
  pub feature_flags: Option<ICPFeatureFlags>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ICPTokens { pub e8s: u64 }

pub type TextAccountIdentifier = String;

#[derive(CandidType, Deserialize, Record)]
pub struct Duration { pub secs: u64, pub nanos: u32 }

#[derive(CandidType, Deserialize, Record)]
pub struct ArchiveOptions {
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
pub struct TextAccountIdentifierTokensPair {
  pub account: TextAccountIdentifier,
  pub amount: ICPTokens,
}

#[derive(CandidType, Deserialize, Record)]
pub struct InitArgs {
  pub send_whitelist: Vec<Principal>,
  pub token_symbol: Option<String>,
  pub transfer_fee: Option<ICPTokens>,
  pub minting_account: TextAccountIdentifier,
  pub maximum_number_of_accounts: Option<u64>,
  pub accounts_overflow_trim_quantity: Option<u64>,
  pub transaction_window: Option<Duration>,
  pub max_message_size_bytes: Option<u64>,
  pub icrc1_minting_account: Option<Account>,
  pub archive_options: Option<ArchiveOptions>,
  pub initial_values: Vec<TextAccountIdentifierTokensPair>,
  pub token_name: Option<String>,
  pub feature_flags: Option<ICPFeatureFlags>,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum LedgerCanisterPayload { Upgrade(Option<UpgradeArgs>), Init(InitArgs) }

pub type AccountIdentifier = serde_bytes::ByteBuf;

#[derive(CandidType, Deserialize, Record)]
pub struct AccountBalanceArgs { pub account: AccountIdentifier }

#[derive(CandidType, Deserialize, Record)]
pub struct AccountBalanceArgsDfx { pub account: TextAccountIdentifier }

#[derive(CandidType, Deserialize, Record)]
pub struct Archive { pub canister_id: Principal }

#[derive(CandidType, Deserialize, Record)]
pub struct Archives { pub archives: Vec<Archive> }

#[derive(CandidType, Deserialize, Record)]
pub struct DecimalsRet { pub decimals: u32 }

pub type Icrc1Tokens = candid::Nat;

// #[derive(CandidType, Deserialize, Record)]
// pub struct StringValuePair { pub key: String, pub value: Value }

// TODO: candid::int not supported in uniffi need to figure this out
// #[derive(CandidType, Deserialize, Enum)]
// pub enum Value {
//   Int(candid::Int),
//   Nat(candid::Nat),
//   Blob(serde_bytes::ByteBuf),
//   Text(String),
// }

#[derive(CandidType, Deserialize, Record)]
pub struct Icrc1SupportedStandardsRetItem { pub url: String, pub name: String }

pub type Icrc1Timestamp = u64;

#[derive(CandidType, Deserialize, Record)]
pub struct ICPTransferArg {
  pub to: Account,
  pub fee: Option<Icrc1Tokens>,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub from_subaccount: Option<SubAccount>,
  pub created_at_time: Option<Icrc1Timestamp>,
  pub amount: Icrc1Tokens,
}

pub type Icrc1BlockIndex = candid::Nat;

#[derive(CandidType, Deserialize, Enum)]
pub enum Icrc1TransferError {
  GenericError{ message: String, error_code: candid::Nat },
  TemporarilyUnavailable,
  BadBurn{ min_burn_amount: Icrc1Tokens },
  Duplicate{ duplicate_of: Icrc1BlockIndex },
  BadFee{ expected_fee: Icrc1Tokens },
  CreatedInFuture{ ledger_time: u64 },
  TooOld,
  InsufficientFunds{ balance: Icrc1Tokens },
}

#[derive(CandidType, Deserialize, Enum)]
pub enum Icrc1TransferResult { Ok(Icrc1BlockIndex), Err(Icrc1TransferError) }

#[derive(CandidType, Deserialize, Record)]
pub struct ICPAllowanceArgs { pub account: Account, pub spender: Account }

#[derive(CandidType, Deserialize, Record)]
pub struct ICPAllowance {
  pub allowance: Icrc1Tokens,
  pub expires_at: Option<Icrc1Timestamp>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct ICPApproveArgs {
  pub fee: Option<Icrc1Tokens>,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub from_subaccount: Option<SubAccount>,
  pub created_at_time: Option<Icrc1Timestamp>,
  pub amount: Icrc1Tokens,
  pub expected_allowance: Option<Icrc1Tokens>,
  pub expires_at: Option<Icrc1Timestamp>,
  pub spender: Account,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ICPApproveError {
  GenericError{ message: String, error_code: candid::Nat },
  TemporarilyUnavailable,
  Duplicate{ duplicate_of: Icrc1BlockIndex },
  BadFee{ expected_fee: Icrc1Tokens },
  AllowanceChanged{ current_allowance: Icrc1Tokens },
  CreatedInFuture{ ledger_time: u64 },
  TooOld,
  Expired{ ledger_time: u64 },
  InsufficientFunds{ balance: Icrc1Tokens },
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ICPApproveResult { Ok(Icrc1BlockIndex), Err(ICPApproveError) }

#[derive(CandidType, Deserialize, Record)]
pub struct ICPTransferFromArgs {
  pub to: Account,
  pub fee: Option<Icrc1Tokens>,
  pub spender_subaccount: Option<SubAccount>,
  pub from: Account,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Icrc1Timestamp>,
  pub amount: Icrc1Tokens,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ICPTransferFromError {
  GenericError{ message: String, error_code: candid::Nat },
  TemporarilyUnavailable,
  InsufficientAllowance{ allowance: Icrc1Tokens },
  BadBurn{ min_burn_amount: Icrc1Tokens },
  Duplicate{ duplicate_of: Icrc1BlockIndex },
  BadFee{ expected_fee: Icrc1Tokens },
  CreatedInFuture{ ledger_time: Icrc1Timestamp },
  TooOld,
  InsufficientFunds{ balance: Icrc1Tokens },
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ICPTransferFromResult { Ok(Icrc1BlockIndex), Err(ICPTransferFromError) }

#[derive(CandidType, Deserialize, Record)]
pub struct NameRet { pub name: String }

pub type BlockIndex = u64;

#[derive(CandidType, Deserialize, Record)]
pub struct ICPGetBlocksArgs { pub start: BlockIndex, pub length: u64 }

pub type Memo = u64;

#[derive(CandidType, Deserialize, Record)]
pub struct TimeStamp { pub timestamp_nanos: u64 }

// #[derive(CandidType, Deserialize, Enum)]
// pub enum ICPOperation {
//   Approve{
//     fee: ICPTokens,
//     from: AccountIdentifier,
//     allowance_e8s: candid::Int,
//     allowance: ICPTokens,
//     expected_allowance: Option<ICPTokens>,
//     expires_at: Option<TimeStamp>,
//     spender: AccountIdentifier,
//   },
//   Burn{
//     from: AccountIdentifier,
//     amount: ICPTokens,
//     spender: Option<AccountIdentifier>,
//   },
//   Mint{ to: AccountIdentifier, amount: ICPTokens },
//   Transfer{
//     to: AccountIdentifier,
//     fee: ICPTokens,
//     from: AccountIdentifier,
//     amount: ICPTokens,
//     spender: Option<serde_bytes::ByteBuf>,
//   },
// }

// #[derive(CandidType, Deserialize, Record)]
// pub struct ICPTransaction {
//   pub memo: Memo,
//   pub icrc1_memo: Option<serde_bytes::ByteBuf>,
//   pub operation: Option<ICPOperation>,
//   pub created_at_time: TimeStamp,
// }

// #[derive(CandidType, Deserialize, Record)]
// pub struct Block {
//   pub transaction: ICPTransaction,
//   pub timestamp: TimeStamp,
//   pub parent_hash: Option<serde_bytes::ByteBuf>,
// }

// #[derive(CandidType, Deserialize, Record)]
// pub struct BlockRange { pub blocks: Vec<Block> }

#[derive(CandidType, Deserialize, Enum)]
pub enum QueryArchiveError {
  BadFirstBlockIndex{
    requested_index: BlockIndex,
    first_valid_index: BlockIndex,
  },
  Other{ error_message: String, error_code: u64 },
}

// #[derive(CandidType, Deserialize, Enum)]
// pub enum QueryArchiveResult { Ok(BlockRange), Err(QueryArchiveError) }

// candid::define_function!(pub QueryArchiveFn : (ICPGetBlocksArgs) -> (
//     QueryArchiveResult,
//   ) query);

// #[derive(CandidType, Deserialize)]
// pub struct ArchivedBlocksRange {
//   pub callback: QueryArchiveFn,
//   pub start: BlockIndex,
//   pub length: u64,
// }

// #[derive(CandidType, Deserialize)]
// pub struct QueryBlocksResponse {
//   pub certificate: Option<serde_bytes::ByteBuf>,
//   pub blocks: Vec<Block>,
//   pub chain_length: u64,
//   pub first_block_index: BlockIndex,
//   pub archived_blocks: Vec<ArchivedBlocksRange>,
// }

#[derive(CandidType, Deserialize, Enum)]
pub enum ArchivedEncodedBlocksRangeCallbackRet {
  Ok(Vec<serde_bytes::ByteBuf>),
  Err(QueryArchiveError),
}

candid::define_function!(pub ArchivedEncodedBlocksRangeCallback : (
    ICPGetBlocksArgs,
  ) -> (ArchivedEncodedBlocksRangeCallbackRet) query);

#[derive(CandidType, Deserialize)]
pub struct ArchivedEncodedBlocksRange {
  pub callback: ArchivedEncodedBlocksRangeCallback,
  pub start: u64,
  pub length: u64,
}

#[derive(CandidType, Deserialize)]
pub struct QueryEncodedBlocksResponse {
  pub certificate: Option<serde_bytes::ByteBuf>,
  pub blocks: Vec<serde_bytes::ByteBuf>,
  pub chain_length: u64,
  pub first_block_index: u64,
  pub archived_blocks: Vec<ArchivedEncodedBlocksRange>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SendArgs {
  pub to: TextAccountIdentifier,
  pub fee: ICPTokens,
  pub memo: Memo,
  pub from_subaccount: Option<SubAccount>,
  pub created_at_time: Option<TimeStamp>,
  pub amount: ICPTokens,
}

#[derive(CandidType, Deserialize, Record)]
pub struct SymbolRet { pub symbol: String }

#[derive(CandidType, Deserialize, Record)]
pub struct TransferArgs {
  pub to: AccountIdentifier,
  pub fee: ICPTokens,
  pub memo: Memo,
  pub from_subaccount: Option<SubAccount>,
  pub created_at_time: Option<TimeStamp>,
  pub amount: ICPTokens,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ICPTransferError {
  TxTooOld{ allowed_window_nanos: u64 },
  BadFee{ expected_fee: ICPTokens },
  TxDuplicate{ duplicate_of: BlockIndex },
  TxCreatedInFuture,
  InsufficientFunds{ balance: ICPTokens },
}

#[derive(CandidType, Deserialize, Enum)]
pub enum ICPTransferResult { Ok(BlockIndex), Err(ICPTransferError) }

#[derive(CandidType, Deserialize, Record)]
pub struct TransferFeeArg {}

#[derive(CandidType, Deserialize, Record)]
pub struct TransferFee { pub transfer_fee: ICPTokens }

#[derive(uniffi::Object)]
pub struct LedgerService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

pub const DEFAULT_LEDGER_CANISTER: &str = "ryjl3-tyaaa-aaaaa-aaaba-cai";

#[uniffi::export]
impl LedgerService {

  #[uniffi::constructor]
  pub fn new(
    principal_text: &str,
    agent_url: &str,
  ) -> std::result::Result<LedgerService, FFIError> {
    let principal = Principal::from_text(principal_text)
          .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
    let agent = Agent::builder()
          .with_url("https://ic0.app/")
          .build()
          .map_err(|e| FFIError::AgentError(format!("Failed to create agent: {:?}", e)))?;    
    Ok(Self {
          principal,
          agent: Arc::new(agent),
      })
  }

  async fn query_canister(&self, method: &str, args: Vec<u8>) -> Result<Vec<u8>> {
    let agent = Arc::clone(&self.agent);
    let principal = Principal::from_text(DEFAULT_LEDGER_CANISTER)
          .map_err(|e| FFIError::PrincipalError(format!("Invalid default principal: {:?}", e)))?;;
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

  #[uniffi::method]
  pub async fn account_balance(&self, arg0: AccountBalanceArgs) -> Result<ICPTokens> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("account_balance", args).await?;
    Ok(Decode!(&bytes, ICPTokens)?)
  }

  #[uniffi::method]
  pub async fn account_balance_dfx(&self, arg0: AccountBalanceArgsDfx) -> Result<ICPTokens> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("account_balance_dfx", args).await?;
    Ok(Decode!(&bytes, ICPTokens)?)
  }

  #[uniffi::method]
  pub async fn account_identifier(&self, arg0: Account) -> Result<AccountIdentifier> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("account_identifier", args).await?;
    Ok(Decode!(&bytes, AccountIdentifier)?)
  }

  #[uniffi::method]
  pub async fn archives(&self) -> Result<Archives> {
    let args = Encode!()?;
    let bytes = self.query_canister("archives", args).await?;
    Ok(Decode!(&bytes, Archives)?)
  }

  #[uniffi::method]
  pub async fn decimals(&self) -> Result<DecimalsRet> {
    let args = Encode!()?;
    let bytes = self.query_canister("decimals", args).await?;
    Ok(Decode!(&bytes, DecimalsRet)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_balance_of(&self, arg0: Account) -> Result<Icrc1Tokens> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("icrc1_balance_of", args).await?;
    Ok(Decode!(&bytes, Icrc1Tokens)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_decimals(&self) -> Result<u8> {
    let args = Encode!()?;
    let bytes = self.query_canister("icrc1_decimals", args).await?;
    Ok(Decode!(&bytes, u8)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_fee(&self) -> Result<Icrc1Tokens> {
    let args = Encode!()?;
    let bytes = self.query_canister("icrc1_fee", args).await?;
    Ok(Decode!(&bytes, Icrc1Tokens)?)
  }

  // #[uniffi::method]
  // pub async fn icrc_1_metadata(&self) -> Result<Vec<StringValuePair>> {
  //   let args = Encode!()?;
  //   let bytes = self.query_canister("icrc1_metadata", args).await?;
  //   Ok(Decode!(&bytes, Vec<StringValuePair>)?)
  // }

  #[uniffi::method]
  pub async fn icrc_1_minting_account(&self) -> Result<Option<Account>> {
    let args = Encode!()?;
    let bytes = self.query_canister("icrc1_minting_account", args).await?;
    Ok(Decode!(&bytes, Option<Account>)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_name(&self) -> Result<String> {
    let args = Encode!()?;
    let bytes = self.query_canister("icrc1_name", args).await?;
    Ok(Decode!(&bytes, String)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_supported_standards(&self) -> Result<Vec<Icrc1SupportedStandardsRetItem>> {
    let args = Encode!()?;
    let bytes = self.query_canister("icrc1_supported_standards", args).await?;
    Ok(Decode!(&bytes, Vec<Icrc1SupportedStandardsRetItem>)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_symbol(&self) -> Result<String> {
    let args = Encode!()?;
    let bytes = self.query_canister("icrc1_symbol", args).await?;
    Ok(Decode!(&bytes, String)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_total_supply(&self) -> Result<Icrc1Tokens> {
    let args = Encode!()?;
    let bytes = self.query_canister("icrc1_total_supply", args).await?;
    Ok(Decode!(&bytes, Icrc1Tokens)?)
  }

  #[uniffi::method]
  pub async fn icrc_1_transfer(&self, arg0: ICPTransferArg) -> Result<Icrc1TransferResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("icrc1_transfer", args).await?;
    Ok(Decode!(&bytes, Icrc1TransferResult)?)
  }

  #[uniffi::method]
  pub async fn icrc_2_allowance(&self, arg0: ICPAllowanceArgs) -> Result<ICPAllowance> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("icrc2_allowance", args).await?;
    Ok(Decode!(&bytes, ICPAllowance)?)
  }

  #[uniffi::method]
  pub async fn icrc_2_approve(&self, arg0: ICPApproveArgs) -> Result<ICPApproveResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("icrc2_approve", args).await?;
    Ok(Decode!(&bytes, ICPApproveResult)?)
  }

  #[uniffi::method]
  pub async fn icrc_2_transfer_from(&self, arg0: ICPTransferFromArgs) -> Result<ICPTransferFromResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("icrc2_transfer_from", args).await?;
    Ok(Decode!(&bytes, ICPTransferFromResult)?)
  }

  #[uniffi::method]
  pub async fn name(&self) -> Result<NameRet> {
    let args = Encode!()?;
    let bytes = self.query_canister("name", args).await?;
    Ok(Decode!(&bytes, NameRet)?)
  }

  // pub async fn query_blocks(&self, arg0: ICPGetBlocksArgs) -> Result<QueryBlocksResponse> {
  //   let args = Encode!(&arg0)?;
  //   let bytes = self.query_canister("query_blocks", args).await?;
  //   Ok(Decode!(&bytes, QueryBlocksResponse)?)
  // }

  // pub async fn query_encoded_blocks(&self, arg0: ICPGetBlocksArgs) -> Result<QueryEncodedBlocksResponse> {
  //   let args = Encode!(&arg0)?;
  //   let bytes = self.query_canister("query_encoded_blocks", args).await?;
  //   Ok(Decode!(&bytes, QueryEncodedBlocksResponse)?)
  // }

  #[uniffi::method]
  pub async fn send_dfx(&self, arg0: SendArgs) -> Result<BlockIndex> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("send_dfx", args).await?;
    Ok(Decode!(&bytes, BlockIndex)?)
  }

  #[uniffi::method]
  pub async fn symbol(&self) -> Result<SymbolRet> {
    let args = Encode!()?;
    let bytes = self.query_canister("symbol", args).await?;
    Ok(Decode!(&bytes, SymbolRet)?)
  }

  #[uniffi::method]
  pub async fn transfer(&self, arg0: TransferArgs) -> Result<ICPTransferResult> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("transfer", args).await?;
    Ok(Decode!(&bytes, ICPTransferResult)?)
  }

  #[uniffi::method]
  pub async fn transfer_fee(&self, arg0: TransferFeeArg) -> Result<TransferFee> {
    let args = Encode!(&arg0)?;
    let bytes = self.query_canister("transfer_fee", args).await?;
    Ok(Decode!(&bytes, TransferFee)?)
  }
}
