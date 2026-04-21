pub mod individual_user_template;
pub mod sns_ledger;
pub mod rate_limits;
pub mod user_post_service;
pub mod user_info_service;
pub mod ledger;
mod uni_ffi_helpers;
mod commons;
mod logger;

lazy_static::lazy_static! {
    static ref RUNTIME: tokio::runtime::Runtime = {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to create Tokio runtime")
    };
}

uniffi::setup_scaffolding!();
