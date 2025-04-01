// This is an experimental feature to generate Rust binding from Candid.
// You may want to manually adjust some of the types.
#![allow(dead_code, unused_imports)]
use crate::individual_user_template;
use crate::RUNTIME;
use candid::{self, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::Agent;
use std::sync::Arc;
type Result<T> = std::result::Result<T, ic_agent::AgentError>;

#[derive(CandidType, Deserialize)]
pub struct InitArgs {
    pub ledger_id: Principal,
}
pub type TxId = candid::Nat;
#[derive(CandidType, Deserialize)]
pub struct Account {
    pub owner: Principal,
    pub subaccount: Option<serde_bytes::ByteBuf>,
}
#[derive(CandidType, Deserialize)]
pub struct GetAccountTransactionsArgs {
    pub max_results: candid::Nat,
    pub start: Option<TxId>,
    pub account: Account,
}
#[derive(CandidType, Deserialize)]
pub struct Burn {
    pub from: Account,
    pub memo: Option<serde_bytes::ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: candid::Nat,
    pub spender: Option<Account>,
}
#[derive(CandidType, Deserialize)]
pub struct Mint {
    pub to: Account,
    pub memo: Option<serde_bytes::ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: candid::Nat,
}
#[derive(CandidType, Deserialize)]
pub struct Approve {
    pub fee: Option<candid::Nat>,
    pub from: Account,
    pub memo: Option<serde_bytes::ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: candid::Nat,
    pub expected_allowance: Option<candid::Nat>,
    pub expires_at: Option<u64>,
    pub spender: Account,
}
#[derive(CandidType, Deserialize)]
pub struct Transfer {
    pub to: Account,
    pub fee: Option<candid::Nat>,
    pub from: Account,
    pub memo: Option<serde_bytes::ByteBuf>,
    pub created_at_time: Option<u64>,
    pub amount: candid::Nat,
    pub spender: Option<Account>,
}
#[derive(CandidType, Deserialize)]
pub struct Transaction {
    pub burn: Option<Burn>,
    pub kind: String,
    pub mint: Option<Mint>,
    pub approve: Option<Approve>,
    pub timestamp: u64,
    pub transfer: Option<Transfer>,
}
#[derive(CandidType, Deserialize)]
pub struct TransactionWithId {
    pub id: TxId,
    pub transaction: Transaction,
}
#[derive(CandidType, Deserialize)]
pub struct GetTransactions {
    pub transactions: Vec<TransactionWithId>,
    pub oldest_tx_id: Option<TxId>,
}
#[derive(CandidType, Deserialize)]
pub struct GetTransactionsErr {
    pub message: String,
}
#[derive(CandidType, Deserialize)]
pub enum GetTransactionsResult {
    Ok(GetTransactions),
    Err(GetTransactionsErr),
}
pub type SubAccount = serde_bytes::ByteBuf;
#[derive(CandidType, Deserialize)]
pub struct ListSubaccountsArgs {
    pub owner: Principal,
    pub start: Option<SubAccount>,
}

pub struct Service {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}
impl Service {
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<Service, PrincipalError> {
        let principal = Principal::from_text(principal_text)?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .build()
            .expect("Failed to create agent");
        RUNTIME
            .block_on(agent.fetch_root_key())
            .expect("Failed to fetch root key");
        Ok(Self {
            principal,
            agent: Arc::new(agent),
        })
    }
    pub async fn get_account_transactions(
        &self,
        arg0: GetAccountTransactionsArgs,
    ) -> Result<GetTransactionsResult> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .update(&self.principal, "get_account_transactions")
            .with_arg(args)
            .call_and_wait()
            .await?;
        Ok(Decode!(&bytes, GetTransactionsResult)?)
    }
    pub async fn ledger_id(&self) -> Result<Principal> {
        let args = Encode!()?;
        let bytes = self
            .agent
            .query(&self.principal, "ledger_id")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, Principal)?)
    }
    pub async fn list_subaccounts(&self, arg0: ListSubaccountsArgs) -> Result<Vec<SubAccount>> {
        let args = Encode!(&arg0)?;
        let bytes = self
            .agent
            .query(&self.principal, "list_subaccounts")
            .with_arg(args)
            .call()
            .await?;
        Ok(Decode!(&bytes, Vec<SubAccount>)?)
    }
}
