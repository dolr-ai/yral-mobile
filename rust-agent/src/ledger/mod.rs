// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
mod ledger_ffi;
use candid::{self, CandidType, Deserialize, Principal};
use ic_cdk::api::call::CallResult as Result;
use ic_cdk::api::call::RejectionCode;


/// Subaccount is an arbitrary 32-byte byte array.
/// Ledger uses subaccounts to compute the source address, which enables one
/// principal to control multiple ledger accounts.
pub type SubAccount = serde_bytes::ByteBuf;
#[derive(CandidType, Deserialize)]
pub struct Account { pub owner: Principal, pub subaccount: Option<SubAccount> }
#[derive(CandidType, Deserialize)]
pub struct FeatureFlags { #[serde(rename="icrc2")] pub icrc_2: bool }
#[derive(CandidType, Deserialize)]
pub struct UpgradeArgs {
  pub maximum_number_of_accounts: Option<u64>,
  #[serde(rename="icrc1_minting_account")]
  pub icrc_1_minting_account: Option<Account>,
  pub feature_flags: Option<FeatureFlags>,
}
/// This is the official Ledger interface that is guaranteed to be backward compatible.
/// Amount of tokens, measured in 10^-8 of a token.
#[derive(CandidType, Deserialize)]
pub struct Tokens { #[serde(rename="e8s")] pub e_8_s: u64 }
/// Account identifier encoded as a 64-byte ASCII hex string.
pub type TextAccountIdentifier = String;
#[derive(CandidType, Deserialize)]
pub struct Duration { pub secs: u64, pub nanos: u32 }
#[derive(CandidType, Deserialize)]
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
#[derive(CandidType, Deserialize)]
pub struct InitArgs {
  pub send_whitelist: Vec<Principal>,
  pub token_symbol: Option<String>,
  pub transfer_fee: Option<Tokens>,
  pub minting_account: TextAccountIdentifier,
  pub maximum_number_of_accounts: Option<u64>,
  pub accounts_overflow_trim_quantity: Option<u64>,
  pub transaction_window: Option<Duration>,
  pub max_message_size_bytes: Option<u64>,
  #[serde(rename="icrc1_minting_account")]
  pub icrc_1_minting_account: Option<Account>,
  pub archive_options: Option<ArchiveOptions>,
  pub initial_values: Vec<(TextAccountIdentifier,Tokens,)>,
  pub token_name: Option<String>,
  pub feature_flags: Option<FeatureFlags>,
}
#[derive(CandidType, Deserialize)]
pub enum LedgerCanisterPayload { Upgrade(Option<UpgradeArgs>), Init(InitArgs) }
/// AccountIdentifier is a 32-byte array.
/// The first 4 bytes is big-endian encoding of a CRC32 checksum of the last 28 bytes.
pub type AccountIdentifier = serde_bytes::ByteBuf;
/// Arguments for the `account_balance` call.
#[derive(CandidType, Deserialize)]
pub struct AccountBalanceArgs { pub account: AccountIdentifier }
#[derive(CandidType, Deserialize)]
pub struct AccountBalanceArgsDfx { pub account: TextAccountIdentifier }
#[derive(CandidType, Deserialize)]
pub struct Archive { pub canister_id: Principal }
#[derive(CandidType, Deserialize)]
pub struct Archives { pub archives: Vec<Archive> }
#[derive(CandidType, Deserialize)]
pub struct DecimalsRet { pub decimals: u32 }
pub type Icrc1Tokens = candid::Nat;
/// The value returned from the [icrc1_metadata] endpoint.
#[derive(CandidType, Deserialize)]
pub enum Value {
  Int(candid::Int),
  Nat(candid::Nat),
  Blob(serde_bytes::ByteBuf),
  Text(String),
}
#[derive(CandidType, Deserialize)]
pub struct Icrc1SupportedStandardsRetItem { pub url: String, pub name: String }
/// Number of nanoseconds since the UNIX epoch in UTC timezone.
pub type Icrc1Timestamp = u64;
#[derive(CandidType, Deserialize)]
pub struct TransferArg {
  pub to: Account,
  pub fee: Option<Icrc1Tokens>,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub from_subaccount: Option<SubAccount>,
  pub created_at_time: Option<Icrc1Timestamp>,
  pub amount: Icrc1Tokens,
}
pub type Icrc1BlockIndex = candid::Nat;
#[derive(CandidType, Deserialize)]
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
pub type Icrc1TransferResult = std::result::Result<
  Icrc1BlockIndex, Icrc1TransferError
>;
#[derive(CandidType, Deserialize)]
pub struct AllowanceArgs { pub account: Account, pub spender: Account }
#[derive(CandidType, Deserialize)]
pub struct Allowance {
  pub allowance: Icrc1Tokens,
  pub expires_at: Option<Icrc1Timestamp>,
}
#[derive(CandidType, Deserialize)]
pub struct ApproveArgs {
  pub fee: Option<Icrc1Tokens>,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub from_subaccount: Option<SubAccount>,
  pub created_at_time: Option<Icrc1Timestamp>,
  pub amount: Icrc1Tokens,
  pub expected_allowance: Option<Icrc1Tokens>,
  pub expires_at: Option<Icrc1Timestamp>,
  pub spender: Account,
}
#[derive(CandidType, Deserialize)]
pub enum ApproveError {
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
pub type ApproveResult = std::result::Result<Icrc1BlockIndex, ApproveError>;
#[derive(CandidType, Deserialize)]
pub struct TransferFromArgs {
  pub to: Account,
  pub fee: Option<Icrc1Tokens>,
  pub spender_subaccount: Option<SubAccount>,
  pub from: Account,
  pub memo: Option<serde_bytes::ByteBuf>,
  pub created_at_time: Option<Icrc1Timestamp>,
  pub amount: Icrc1Tokens,
}
#[derive(CandidType, Deserialize)]
pub enum TransferFromError {
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
pub type TransferFromResult = std::result::Result<
  Icrc1BlockIndex, TransferFromError
>;
#[derive(CandidType, Deserialize)]
pub struct NameRet { pub name: String }
/// Sequence number of a block produced by the ledger.
pub type BlockIndex = u64;
#[derive(CandidType, Deserialize)]
pub struct GetBlocksArgs {
  /// The index of the first block to fetch.
  pub start: BlockIndex,
  /// Max number of blocks to fetch.
  pub length: u64,
}
/// An arbitrary number associated with a transaction.
/// The caller can set it in a `transfer` call as a correlation identifier.
pub type Memo = u64;
/// Number of nanoseconds from the UNIX epoch in UTC timezone.
#[derive(CandidType, Deserialize)]
pub struct TimeStamp { pub timestamp_nanos: u64 }
#[derive(CandidType, Deserialize)]
pub enum Operation {
  Approve{
    fee: Tokens,
    from: AccountIdentifier,
    /// This field is deprecated and should not be used.
    #[serde(rename="allowance_e8s")]
    allowance_e_8_s: candid::Int,
    allowance: Tokens,
    expected_allowance: Option<Tokens>,
    expires_at: Option<TimeStamp>,
    spender: AccountIdentifier,
  },
  Burn{
    from: AccountIdentifier,
    amount: Tokens,
    spender: Option<AccountIdentifier>,
  },
  Mint{ to: AccountIdentifier, amount: Tokens },
  Transfer{
    to: AccountIdentifier,
    fee: Tokens,
    from: AccountIdentifier,
    amount: Tokens,
    spender: Option<serde_bytes::ByteBuf>,
  },
}
#[derive(CandidType, Deserialize)]
pub struct Transaction {
  pub memo: Memo,
  #[serde(rename="icrc1_memo")]
  pub icrc_1_memo: Option<serde_bytes::ByteBuf>,
  pub operation: Option<Operation>,
  pub created_at_time: TimeStamp,
}
#[derive(CandidType, Deserialize)]
pub struct Block {
  pub transaction: Transaction,
  pub timestamp: TimeStamp,
  pub parent_hash: Option<serde_bytes::ByteBuf>,
}
/// A prefix of the block range specified in the [GetBlocksArgs] request.
#[derive(CandidType, Deserialize)]
pub struct BlockRange {
  /// A prefix of the requested block range.
  /// The index of the first block is equal to [GetBlocksArgs.from].
  /// 
  /// Note that the number of blocks might be less than the requested
  /// [GetBlocksArgs.len] for various reasons, for example:
  /// 
  /// 1. The query might have hit the replica with an outdated state
  /// that doesn't have the full block range yet.
  /// 2. The requested range is too large to fit into a single reply.
  /// 
  /// NOTE: the list of blocks can be empty if:
  /// 1. [GetBlocksArgs.len] was zero.
  /// 2. [GetBlocksArgs.from] was larger than the last block known to the canister.
  pub blocks: Vec<Block>,
}
/// An error indicating that the arguments passed to [QueryArchiveFn] were invalid.
#[derive(CandidType, Deserialize)]
pub enum QueryArchiveError {
  /// [GetBlocksArgs.from] argument was smaller than the first block
  /// served by the canister that received the request.
  BadFirstBlockIndex{
    requested_index: BlockIndex,
    first_valid_index: BlockIndex,
  },
  /// Reserved for future use.
  Other{ error_message: String, error_code: u64 },
}
pub type QueryArchiveResult = std::result::Result<
  BlockRange, QueryArchiveError
>;
candid::define_function!(pub QueryArchiveFn : (GetBlocksArgs) -> (
    QueryArchiveResult,
  ) query);
#[derive(CandidType, Deserialize)]
pub struct ArchivedBlocksRange {
  /// The function that should be called to fetch the archived blocks.
  /// The range of the blocks accessible using this function is given by [from]
  /// and [len] fields above.
  pub callback: QueryArchiveFn,
  /// The index of the first archived block that can be fetched using the callback.
  pub start: BlockIndex,
  /// The number of blocks that can be fetch using the callback.
  pub length: u64,
}
/// The result of a "query_blocks" call.
/// 
/// The structure of the result is somewhat complicated because the main ledger canister might
/// not have all the blocks that the caller requested: One or more "archive" canisters might
/// store some of the requested blocks.
/// 
/// Note: as of Q4 2021 when this interface is authored, the IC doesn't support making nested
/// query calls within a query call.
#[derive(CandidType, Deserialize)]
pub struct QueryBlocksResponse {
  /// System certificate for the hash of the latest block in the chain.
  /// Only present if `query_blocks` is called in a non-replicated query context.
  pub certificate: Option<serde_bytes::ByteBuf>,
  /// List of blocks that were available in the ledger when it processed the call.
  /// 
  /// The blocks form a contiguous range, with the first block having index
  /// [first_block_index] (see below), and the last block having index
  /// [first_block_index] + len(blocks) - 1.
  /// 
  /// The block range can be an arbitrary sub-range of the originally requested range.
  pub blocks: Vec<Block>,
  /// The total number of blocks in the chain.
  /// If the chain length is positive, the index of the last block is `chain_len - 1`.
  pub chain_length: u64,
  /// The index of the first block in "blocks".
  /// If the blocks vector is empty, the exact value of this field is not specified.
  pub first_block_index: BlockIndex,
  /// Encoding of instructions for fetching archived blocks whose indices fall into the
  /// requested range.
  /// 
  /// For each entry `e` in [archived_blocks], `[e.from, e.from + len)` is a sub-range
  /// of the originally requested block range.
  pub archived_blocks: Vec<ArchivedBlocksRange>,
}
candid::define_function!(pub ArchivedEncodedBlocksRangeCallback : (
    GetBlocksArgs,
  ) -> (
    std::result::Result<Vec<serde_bytes::ByteBuf>, QueryArchiveError>,
  ) query);
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
/// Arguments for the `send_dfx` call.
#[derive(CandidType, Deserialize)]
pub struct SendArgs {
  pub to: TextAccountIdentifier,
  pub fee: Tokens,
  pub memo: Memo,
  pub from_subaccount: Option<SubAccount>,
  pub created_at_time: Option<TimeStamp>,
  pub amount: Tokens,
}
#[derive(CandidType, Deserialize)]
pub struct SymbolRet { pub symbol: String }
/// Arguments for the `transfer` call.
#[derive(CandidType, Deserialize)]
pub struct TransferArgs {
  /// The destination account.
  /// If the transfer is successful, the balance of this address increases by `amount`.
  pub to: AccountIdentifier,
  /// The amount that the caller pays for the transaction.
  /// Must be 10000 e8s.
  pub fee: Tokens,
  /// Transaction memo.
  /// See comments for the `Memo` type.
  pub memo: Memo,
  /// The subaccount from which the caller wants to transfer funds.
  /// If null, the ledger uses the default (all zeros) subaccount to compute the source address.
  /// See comments for the `SubAccount` type.
  pub from_subaccount: Option<SubAccount>,
  /// The point in time when the caller created this request.
  /// If null, the ledger uses current IC time as the timestamp.
  pub created_at_time: Option<TimeStamp>,
  /// The amount that the caller wants to transfer to the destination address.
  pub amount: Tokens,
}
#[derive(CandidType, Deserialize)]
pub enum TransferError {
  /// The request is too old.
  /// The ledger only accepts requests created within 24 hours window.
  /// This is a non-recoverable error.
  TxTooOld{ allowed_window_nanos: u64 },
  /// The fee that the caller specified in the transfer request was not the one that ledger expects.
  /// The caller can change the transfer fee to the `expected_fee` and retry the request.
  BadFee{ expected_fee: Tokens },
  /// The ledger has already executed the request.
  /// `duplicate_of` field is equal to the index of the block containing the original transaction.
  TxDuplicate{ duplicate_of: BlockIndex },
  /// The caller specified `created_at_time` that is too far in future.
  /// The caller can retry the request later.
  TxCreatedInFuture,
  /// The account specified by the caller doesn't have enough funds.
  InsufficientFunds{ balance: Tokens },
}
pub type TransferResult = std::result::Result<BlockIndex, TransferError>;
#[derive(CandidType, Deserialize)]
pub struct TransferFeeArg {}
#[derive(CandidType, Deserialize)]
pub struct TransferFee {
  /// The fee to pay to perform a transfer
  pub transfer_fee: Tokens,
}

pub struct LedgerService(pub Principal);
impl LedgerService {
  /// Returns the amount of Tokens on the specified account.
  pub async fn account_balance(&self, arg0: &AccountBalanceArgs) -> Result<(Tokens,)> {
    ic_cdk::call(self.0, "account_balance", (arg0,)).await
  }
  pub async fn account_balance_dfx(&self, arg0: &AccountBalanceArgsDfx) -> Result<(Tokens,)> {
    ic_cdk::call(self.0, "account_balance_dfx", (arg0,)).await
  }
  /// Returns the account identifier for the given Principal and subaccount.
  pub async fn account_identifier(&self, arg0: &Account) -> Result<(AccountIdentifier,)> {
    ic_cdk::call(self.0, "account_identifier", (arg0,)).await
  }
  /// Returns the existing archive canisters information.
  pub async fn archives(&self) -> Result<(Archives,)> {
    ic_cdk::call(self.0, "archives", ()).await
  }
  /// Returns token decimals.
  pub async fn decimals(&self) -> Result<(DecimalsRet,)> {
    ic_cdk::call(self.0, "decimals", ()).await
  }
  pub async fn icrc_1_balance_of(&self, arg0: &Account) -> Result<(Icrc1Tokens,)> {
    ic_cdk::call(self.0, "icrc1_balance_of", (arg0,)).await
  }
  pub async fn icrc_1_balance_of_sb(
    &self,
    account: &Account
) -> std::result::Result<Icrc1Tokens, BalanceError> {
    match self.icrc_1_balance_of(account).await {
        Ok((nat,)) => Ok(nat),              // <-- unwrap the single-element tuple
        Err(e) => Err(e.into()),
    }
}
  pub async fn icrc_1_decimals(&self) -> Result<(u8,)> {
    ic_cdk::call(self.0, "icrc1_decimals", ()).await
  }
  pub async fn icrc_1_fee(&self) -> Result<(Icrc1Tokens,)> {
    ic_cdk::call(self.0, "icrc1_fee", ()).await
  }
  pub async fn icrc_1_metadata(&self) -> Result<(Vec<(String,Value,)>,)> {
    ic_cdk::call(self.0, "icrc1_metadata", ()).await
  }
  pub async fn icrc_1_minting_account(&self) -> Result<(Option<Account>,)> {
    ic_cdk::call(self.0, "icrc1_minting_account", ()).await
  }
  /// The following methods implement the ICRC-1 Token Standard.
  /// https://github.com/dfinity/ICRC-1/tree/main/standards/ICRC-1
  pub async fn icrc_1_name(&self) -> Result<(String,)> {
    ic_cdk::call(self.0, "icrc1_name", ()).await
  }
  pub async fn icrc_1_supported_standards(&self) -> Result<(Vec<Icrc1SupportedStandardsRetItem>,)> {
    ic_cdk::call(self.0, "icrc1_supported_standards", ()).await
  }
  pub async fn icrc_1_symbol(&self) -> Result<(String,)> {
    ic_cdk::call(self.0, "icrc1_symbol", ()).await
  }
  pub async fn icrc_1_total_supply(&self) -> Result<(Icrc1Tokens,)> {
    ic_cdk::call(self.0, "icrc1_total_supply", ()).await
  }
  pub async fn icrc_1_transfer(&self, arg0: &TransferArg) -> Result<(Icrc1TransferResult,)> {
    ic_cdk::call(self.0, "icrc1_transfer", (arg0,)).await
  }
  pub async fn icrc_2_allowance(&self, arg0: &AllowanceArgs) -> Result<(Allowance,)> {
    ic_cdk::call(self.0, "icrc2_allowance", (arg0,)).await
  }
  pub async fn icrc_2_approve(&self, arg0: &ApproveArgs) -> Result<(ApproveResult,)> {
    ic_cdk::call(self.0, "icrc2_approve", (arg0,)).await
  }
  pub async fn icrc_2_transfer_from(&self, arg0: &TransferFromArgs) -> Result<(TransferFromResult,)> {
    ic_cdk::call(self.0, "icrc2_transfer_from", (arg0,)).await
  }
  /// Returns token name.
  pub async fn name(&self) -> Result<(NameRet,)> {
    ic_cdk::call(self.0, "name", ()).await
  }
  /// Queries blocks in the specified range.
  pub async fn query_blocks(&self, arg0: &GetBlocksArgs) -> Result<(QueryBlocksResponse,)> {
    ic_cdk::call(self.0, "query_blocks", (arg0,)).await
  }
  /// Queries encoded blocks in the specified range
  pub async fn query_encoded_blocks(&self, arg0: &GetBlocksArgs) -> Result<(QueryEncodedBlocksResponse,)> {
    ic_cdk::call(self.0, "query_encoded_blocks", (arg0,)).await
  }
  pub async fn send_dfx(&self, arg0: &SendArgs) -> Result<(BlockIndex,)> {
    ic_cdk::call(self.0, "send_dfx", (arg0,)).await
  }
  /// Returns token symbol.
  pub async fn symbol(&self) -> Result<(SymbolRet,)> {
    ic_cdk::call(self.0, "symbol", ()).await
  }
  /// Transfers tokens from a subaccount of the caller to the destination address.
  /// The source address is computed from the principal of the caller and the specified subaccount.
  /// When successful, returns the index of the block containing the transaction.
  pub async fn transfer(&self, arg0: &TransferArgs) -> Result<(TransferResult,)> {
    ic_cdk::call(self.0, "transfer", (arg0,)).await
  }
  /// Returns the current transfer_fee.
  pub async fn transfer_fee(&self, arg0: &TransferFeeArg) -> Result<(TransferFee,)> {
    ic_cdk::call(self.0, "transfer_fee", (arg0,)).await
  }

  pub fn ledger_new_from_text(principal_text: &str) -> Option<LedgerService> {
    let pid = Principal::from_text(principal_text).ok()?;
    Some(LedgerService(pid))
}
}

#[derive(Debug)]
pub struct BalanceError {
    pub code: i32,
    pub message: String,
}

impl From<(RejectionCode, String)> for BalanceError {
    fn from((code, msg): (RejectionCode, String)) -> Self {
        Self { code: code as i32, message: msg }
    }
}
