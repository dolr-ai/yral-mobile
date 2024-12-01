use crate::sns_index::*;
use ic_agent::AgentError;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type InitArgs;
        type TxId;
        #[swift_bridge(already_declared)]
        type Account;
        type GetAccountTransactionsArgs;
        type Burn;
        type Mint;
        type Approve;
        type Transfer;
        type Transaction;
        type TransactionWithId;
        type GetTransactions;
        type GetTransactionsErr;
        type GetTransactionsResult;
        type SubAccount;
        type ListSubaccountsArgs;
        type Service;
        #[swift_bridge(already_declared)]
        type AgentError;
        #[swift_bridge(already_declared)]
        type Principal;
    }
    extern "Rust" {
        #[swift_bridge(already_declared)]
        type Service;
        async fn get_account_transactions(
            &self,
            arg0: GetAccountTransactionsArgs,
        ) -> Result<GetTransactionsResult, AgentError>;
        async fn ledger_id(&self) -> Result<Principal, AgentError>;
        async fn list_subaccounts(
            &self,
            arg0: ListSubaccountsArgs,
        ) -> Result<Vec<SubAccount>, AgentError>;
    }
}
