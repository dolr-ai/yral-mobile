pub mod individual_user_template;
pub mod platform_orchestrator;
pub mod post_cache;
pub mod sns_governance;
pub mod sns_index;
pub mod sns_ledger;
pub mod sns_root;
pub mod sns_swap;
use ic_agent::Agent;

use individual_user_template::*;
use platform_orchestrator::*;
use post_cache::*;
use sns_governance::*;
use sns_index::*;
use sns_ledger::*;
use sns_root::*;
use sns_swap::*;

lazy_static::lazy_static! {
    static ref RUNTIME: tokio::runtime::Runtime = {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to create Tokio runtime")
    };
}
