pub use crate::ledger::{LedgerService, Account, Icrc1Tokens, BalanceError};
use candid::Principal;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type LedgerService;
        #[swift_bridge(init, rust_name = "ledger_new_from_text")]
        fn new_from_text(principal_text: &str) -> Option<LedgerService>;
        async fn icrc_1_balance_of_sb(
            &self,
            account: &Account
        ) -> Result<Icrc1Tokens, BalanceError>;
    }
    extern "Rust" {
        type Account;
        type Icrc1Tokens;
        type BalanceError;
        #[swift_bridge(already_declared)]
        type Principal;
    }
}
