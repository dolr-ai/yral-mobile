pub use crate::ledger::{LedgerService, Account, Icrc1Tokens, BalanceError};
use candid::Principal;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type LedgerService;
        #[swift_bridge(init, rust_name = "ledger_new_from_text")]
        fn new_from_text(principal_text: &str) -> Option<LedgerService>;
        async fn icrc_1_balance_of(
            &self,
            arg0: &Account
          ) -> Result<u32, String>;
    }
    extern "Rust" {
        type Account;
        #[swift_bridge(init, rust_name = "new_from_text")]
        fn new_from_text(
            owner_text: &str,
        ) -> Option<Account>;
    }
    extern "Rust" {
        type Icrc1Tokens;
        type BalanceError;
        #[swift_bridge(already_declared)]
        type Principal;
    }
}
