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
use crate::commons::{Account, Approve, Burn, Mint, SubAccount, Transaction, TxId};

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct SnsIndexInitArgs {
    pub ledger_id: Principal,
}


#[derive(CandidType, Deserialize, Record)]
pub struct GetAccountTransactionsArgs {
    pub max_results: candid::Nat,
    pub start: Option<TxId>,
    pub account: Account,
}

#[derive(CandidType, Deserialize, Record)]
pub struct TransactionWithId {
    pub id: TxId,
    pub transaction: Transaction,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetTransactions {
    pub transactions: Vec<TransactionWithId>,
    pub oldest_tx_id: Option<TxId>,
}

#[derive(CandidType, Deserialize, Record)]
pub struct GetTransactionsErr {
    pub message: String,
}

#[derive(CandidType, Deserialize, Enum)]
pub enum GetTransactionsResult {
    Ok(GetTransactions),
    Err(GetTransactionsErr),
}


#[derive(CandidType, Deserialize, Record)]
pub struct ListSubaccountsArgs {
    pub owner: Principal,
    pub start: Option<SubAccount>,
}

#[derive(uniffi::Object)]
pub struct SnsIndexService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl SnsIndexService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        agent_url: &str,
    ) -> std::result::Result<SnsIndexService, FFIError> {
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
    pub async fn get_account_transactions(
        &self,
        arg0: GetAccountTransactionsArgs,
    ) -> Result<GetTransactionsResult> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("get_account_transactions", args).await?;
        Ok(Decode!(&bytes, GetTransactionsResult)?)
    }

    #[uniffi::method]
    pub async fn ledger_id(&self) -> Result<Principal> {
        let args = Encode!()?;
        let bytes = self.query_canister("ledger_id", args).await?;
        Ok(Decode!(&bytes, Principal)?)
    }

    #[uniffi::method]
    pub async fn list_subaccounts(&self, arg0: ListSubaccountsArgs) -> Result<Vec<SubAccount>> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("list_subaccounts", args).await?;
        Ok(Decode!(&bytes, Vec<SubAccount>)?)
    }
}
