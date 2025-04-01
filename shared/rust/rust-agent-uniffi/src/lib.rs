pub mod individual_user_template;
pub mod sns_swap;
pub mod sns_root;
pub mod sns_ledger;
pub mod sns_index;
pub mod sns_governance;
pub mod post_cache;
pub mod platform_orchestrator;
mod uni_ffi_helpers;

lazy_static::lazy_static! {
    static ref RUNTIME: tokio::runtime::Runtime = {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to create Tokio runtime")
    };
}

uniffi::setup_scaffolding!();