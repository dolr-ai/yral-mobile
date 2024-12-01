use crate::post_cache::*;
use candid::Nat;
use ic_agent::export::PrincipalError;
use ic_agent::AgentError;
use serde_bytes::ByteBuf;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        #[swift_bridge(already_declared)]
        type KnownPrincipalType;
        type PostCacheInitArgs;
        #[swift_bridge(already_declared)]
        type PostStatus;
        type NsfwFilter;
        #[swift_bridge(already_declared)]
        type SystemTime;
        type PostScoreIndexItemV1;
        type TopPostsFetchError;
        type PostCacheResult_;
        #[swift_bridge(already_declared)]
        type HttpRequest;
        #[swift_bridge(already_declared)]
        type HttpResponse;
        #[swift_bridge(already_declared)]
        type Principal;
        #[swift_bridge(already_declared)]
        type Nat;
        #[swift_bridge(already_declared)]
        type AgentError;
    }

    extern "Rust" {
        #[swift_bridge(already_declared)]
        type Service;

        async fn get_top_posts_aggregated_from_canisters_on_this_network_for_home_feed_cursor(
            &self,
            arg0: u64,
            arg1: u64,
            arg2: Option<bool>,
            arg3: Option<PostStatus>,
            arg4: Option<NsfwFilter>,
        ) -> Result<PostCacheResult_, AgentError>;

        async fn get_top_posts_aggregated_from_canisters_on_this_network_for_hot_or_not_feed_cursor(
            &self,
            arg0: u64,
            arg1: u64,
            arg2: Option<bool>,
            arg3: Option<PostStatus>,
            arg4: Option<NsfwFilter>,
        ) -> Result<PostCacheResult_, AgentError>;

        async fn receive_top_home_feed_posts_from_publishing_canister(
            &self,
            arg0: Vec<PostScoreIndexItemV1>,
        ) -> Result<(), AgentError>;

        async fn receive_top_hot_or_not_feed_posts_from_publishing_canister(
            &self,
            arg0: Vec<PostScoreIndexItemV1>,
        ) -> Result<(), AgentError>;

        async fn remove_all_feed_entries(&self) -> Result<(), AgentError>;

        async fn update_post_home_feed(&self, arg0: PostScoreIndexItemV1)
            -> Result<(), AgentError>;

        async fn update_post_hot_or_not_feed(
            &self,
            arg0: PostScoreIndexItemV1,
        ) -> Result<(), AgentError>;
    }
}
