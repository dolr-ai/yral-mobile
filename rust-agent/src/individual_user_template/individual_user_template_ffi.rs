use std::str::FromStr;
use crate::{individual_user_template::*, Err};
use candid::{self, ser, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::AgentError;
use serde_bytes::ByteBuf;
use candid::Nat;
use k256::elliptic_curve::JwkEcKey;
use ic_agent::identity::Secp256k1Identity;
use yral_types::delegated_identity::DelegatedIdentityWire;
use ic_agent::identity::DelegatedIdentity;
use yral_canisters_common::Canisters;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type Result_;
        type PostDetailsFromFrontend;
        type RejectionCode;
        type TransferError;
        type CdaoTokenError;
        type Result1;
        type Result2;
        type BetDirection;
        type PlaceBetArg;
        type SystemTime;
        type BettingStatus;
        type BetOnCurrentlyViewingPostError;
        type Result3;
        type NamespaceForFrontend;
        type NamespaceErrors;
        type Result4;
        type Result5;
        type Result6;
        type NeuronBasketConstructionParameters;
        type Canister;
        type DappCanisters;
        type LinearScalingCoefficient;
        type IdealMatchedParticipationFunction;
        type NeuronsFundParticipationConstraints;
        type CfNeuron;
        type CfParticipant;
        type NeuronsFundParticipants;
        type TreasuryDistribution;
        type NeuronDistribution;
        type DeveloperDistribution;
        type AirdropDistribution;
        type SwapDistribution;
        type FractionalDeveloperVotingPower;
        type InitialTokenDistribution;
        type Countries;
        type SnsInitPayload;
        type DeployedCdaoCanisters;
        type CdaoDeployError;
        type Result7;
        type FolloweeArg;
        type FollowAnotherUserProfileError;
        type Result8;
        type BetMakerInformedStatus;
        type BetPayout;
        type BetDetails;
        type Result9;
        type DeviceIdentity;
        type PostStatus;
        type FeedScore;
        type PostViewStatistics;
        type AggregateStats;
        type RoomBetPossibleOutcomes;
        type RoomDetails;
        type SlotDetails;
        type HotOrNotDetails;
        type Post;
        type Result10;
        type BetOutcomeForBetMaker;
        type PlacedBetDetail;
        type PostDetailsForFrontend;
        type Result11;
        type MlFeedCacheItem;
        type GetPostsOfUserProfileError;
        type Result12;
        type FollowEntryDetail;
        type FollowEntry;
        type UserProfileGlobalStats;
        type UserCanisterDetails;
        type UserProfileDetailsForFrontend;
        type MigrationInfo;
        type UserProfileDetailsForFrontendV2;
        type SessionType;
        type Result13;
        type SuccessHistoryItemV1;
        type Result14;
        type PaginationError;
        type Result15;
        type StakeEvent;
        type MintEvent;
        type HotOrNotOutcomePayoutEvent;
        type TokenEvent;
        type Result16;
        type WatchHistoryItem;
        type Result17;
        type KnownPrincipalType;
        type HttpRequest;
        type HttpResponse;
        type Result18;
        type Result19;
        type MigrationErrors;
        type Result20;
        type Committed;
        type Result21;
        type SettleNeuronsFundParticipationRequest;
        type NeuronsFundNeuron;
        type Ok;
        type GovernanceError;
        type Result22;
        type SettleNeuronsFundParticipationResponse;
        type Result23;
        type Result24;
        type PostViewDetailsFromFrontend;
        type UserProfileUpdateDetailsFromFrontend;
        type UpdateProfileDetailsError;
        type Result25;
        type Result26;
        type UpdateProfileSetUniqueUsernameError;
        type Result27;
        type FollowerArg;
        type AgentError;
        type PrincipalError;
        type Principal;
        type ByteBuf;
        type Nat;
        type JwkEcKey;
        type Secp256k1Identity;
        type DelegatedIdentity;
    }

    extern "Rust" {
        type Service;
        #[swift_bridge(init)]
        fn new(principal: Principal, identity: DelegatedIdentity) -> Result<Service, PrincipalError>;
        async fn add_device_id(&self, arg0: String) -> Result<Result_, AgentError>;
        async fn add_post_v_2(&self, arg0: PostDetailsFromFrontend) -> Result<Result1, AgentError>;
        async fn add_token(&self, arg0: Principal) -> Result<Result2, AgentError>;
        async fn bet_on_currently_viewing_post(&self, arg0: PlaceBetArg) -> Result<Result3, AgentError>;
        async fn check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold(
            &self,
            arg0: Vec<u64>,
        ) -> Result<(), AgentError>;
        async fn clear_snapshot(&self) -> Result<(), AgentError>;
        async fn create_a_namespace(&self, arg0: String) -> Result<Result4, AgentError>;
        async fn delete_key_value_pair(&self, arg0: u64, arg1: String) -> Result<Result5, AgentError>;
        async fn delete_multiple_key_value_pairs(
            &self,
            arg0: u64,
            arg1: Vec<String>,
        ) -> Result<Result6, AgentError>;
        async fn deploy_cdao_sns(&self, arg0: SnsInitPayload, arg1: u64) -> Result<Result7, AgentError>;
        async fn deployed_cdao_canisters(&self) -> Result<Vec<DeployedCdaoCanisters>, AgentError>;
        async fn do_i_follow_this_user(&self, arg0: FolloweeArg) -> Result<Result8, AgentError>;
        //async fn download_snapshot(&self, arg0: u64, arg1: u64) -> Result<serde_bytes::ByteBuf, AgentError>;
        async fn get_bet_details_for_a_user_on_a_post(
            &self,
            arg0: Principal,
            arg1: u64,
        ) -> Result<Result9, AgentError>;
        async fn get_device_identities(&self) -> Result<Vec<DeviceIdentity>, AgentError>;
        async fn get_entire_individual_post_detail_by_id(&self, arg0: u64) -> Result<Result10, AgentError>;
        async fn get_hot_or_not_bet_details_for_this_post(
            &self,
            arg0: u64,
        ) -> Result<BettingStatus, AgentError>;
        async fn get_hot_or_not_bets_placed_by_this_profile_with_pagination(
            &self,
            arg0: u64,
        ) -> Result<Vec<PlacedBetDetail>, AgentError>;
        async fn get_individual_post_details_by_id(
            &self,
            arg0: u64,
        ) -> Result<PostDetailsForFrontend, AgentError>;
        // async fn get_individual_hot_or_not_bet_placed_by_this_profile(
        //     &self,
        //     arg0: Principal,
        //     arg1: u64,
        // ) -> Result<Option<PlacedBetDetail>, AgentError>;
        async fn get_last_access_time(&self) -> Result<Result11, AgentError>;
        async fn get_last_canister_functionality_access_time(&self) -> Result<Result11, AgentError>;
        async fn get_ml_feed_cache_paginated(
            &self,
            arg0: u64,
            arg1: u64,
        ) -> Result<Vec<MlFeedCacheItem>, AgentError>;
        async fn get_posts_of_this_user_profile_with_pagination(
            &self,
            arg0: u64,
            arg1: u64,
        ) -> Result<Result12, AgentError>;
        async fn get_posts_of_this_user_profile_with_pagination_cursor(
            &self,
            arg0: u64,
            arg1: u64,
        ) -> Result<Result12, AgentError>;
        async fn get_principals_that_follow_this_profile_paginated(
            &self,
            arg0: Option<u64>,
        ) -> Result<Vec<FollowEntry>, AgentError>;
        async fn get_principals_this_profile_follows_paginated(
            &self,
            arg0: Option<u64>,
        ) -> Result<Vec<FollowEntry>, AgentError>;
        async fn get_profile_details(&self) -> Result<UserProfileDetailsForFrontend, AgentError>;
        async fn get_profile_details_v_2(&self) -> Result<UserProfileDetailsForFrontendV2, AgentError>;
        async fn get_rewarded_for_referral(&self, arg0: Principal, arg1: Principal) -> Result<(), AgentError>;
        async fn get_rewarded_for_signing_up(&self) -> Result<(), AgentError>;
        async fn get_session_type(&self) -> Result<Result13, AgentError>;
        // async fn get_stable_memory_size(&self) -> Result<u64, AgentError>;
        async fn get_success_history(&self) -> Result<Result14, AgentError>;
        async fn get_token_roots_of_this_user_with_pagination_cursor(
            &self,
            arg0: u64,
            arg1: u64,
        ) -> Result<Result15, AgentError>;
        async fn get_user_caniser_cycle_balance(&self) -> Result<Nat, AgentError>;
        async fn get_user_utility_token_transaction_history_with_pagination(
            &self,
            arg0: u64,
            arg1: u64,
        ) -> Result<Result16, AgentError>;
        // async fn get_utility_token_balance(&self) -> Result<u64, AgentError>;
        async fn get_version(&self) -> Result<String, AgentError>;
        // async fn get_version_number(&self) -> Result<u64, AgentError>;
        async fn get_watch_history(&self) -> Result<Result17, AgentError>;
        // async fn get_well_known_principal_value(
        //     &self,
        //     arg0: KnownPrincipalType,
        // ) -> Result<Option<Principal>, AgentError>;
        async fn http_request(&self, arg0: HttpRequest) -> Result<HttpResponse, AgentError>;
        async fn list_namespace_keys(&self, arg0: u64) -> Result<Result18, AgentError>;
        async fn list_namespaces(&self, arg0: u64, arg1: u64) -> Result<Vec<NamespaceForFrontend>, AgentError>;
        async fn load_snapshot(&self, arg0: u64) -> Result<(), AgentError>;
        async fn once_reenqueue_timers_for_pending_bet_outcomes(&self) -> Result<Result19, AgentError>;
        async fn read_key_value_pair(&self, arg0: u64, arg1: String) -> Result<Result5, AgentError>;
        async fn receive_and_save_snaphot(
            &self,
            arg0: u64,
            arg1: ByteBuf,
        ) -> Result<(), AgentError>;
        async fn receive_bet_from_bet_makers_canister(
            &self,
            arg0: PlaceBetArg,
            arg1: Principal,
        ) -> Result<Result3, AgentError>;
        async fn receive_bet_winnings_when_distributed(
            &self,
            arg0: u64,
            arg1: BetOutcomeForBetMaker,
        ) -> Result<(), AgentError>;
        async fn receive_data_from_hotornot(
            &self,
            arg0: Principal,
            arg1: u64,
            arg2: Vec<Post>,
        ) -> Result<Result20, AgentError>;
        async fn return_cycles_to_user_index_canister(
            &self,
            arg0: Option<Nat>,
        ) -> Result<(), AgentError>;
        async fn save_snapshot_json(&self) -> Result<u32, AgentError>;
        async fn settle_neurons_fund_participation(
            &self,
            arg0: SettleNeuronsFundParticipationRequest,
        ) -> Result<SettleNeuronsFundParticipationResponse, AgentError>;
        async fn transfer_token_to_user_canister(
            &self,
            arg0: Principal,
            arg1: Principal,
            arg2: Option<ByteBuf>,
            arg3: Nat,
        ) -> Result<Result23, AgentError>;
        async fn transfer_tokens_and_posts(
            &self,
            arg0: Principal,
            arg1: Principal,
        ) -> Result<Result20, AgentError>;
        async fn update_last_access_time(&self) -> Result<Result24, AgentError>;
        async fn update_last_canister_functionality_access_time(&self) -> Result<(), AgentError>;
        async fn update_ml_feed_cache(&self, arg0: Vec<MlFeedCacheItem>) -> Result<Result24, AgentError>;
        async fn update_post_add_view_details(
            &self,
            arg0: u64,
            arg1: PostViewDetailsFromFrontend,
        ) -> Result<(), AgentError>;
        async fn update_post_as_ready_to_view(&self, arg0: u64) -> Result<(), AgentError>;
        // async fn update_post_increment_share_count(&self, arg0: u64) -> Result<u64, AgentError>;
        async fn update_post_status(&self, arg0: u64, arg1: PostStatus) -> Result<(), AgentError>;
        async fn update_profile_display_details(
            &self,
            arg0: UserProfileUpdateDetailsFromFrontend,
        ) -> Result<Result25, AgentError>;
        async fn update_profile_owner(&self, arg0: Option<Principal>) -> Result<Result26, AgentError>;
        async fn update_profile_set_unique_username_once(&self, arg0: String) -> Result<Result27, AgentError>;
        async fn update_profiles_i_follow_toggle_list_with_specified_profile(
            &self,
            arg0: FolloweeArg,
        ) -> Result<Result8, AgentError>;
        async fn update_profiles_that_follow_me_toggle_list_with_specified_profile(
            &self,
            arg0: FollowerArg,
        ) -> Result<Result8, AgentError>;
        async fn update_referrer_details(&self, arg0: UserCanisterDetails) -> Result<Result24, AgentError>;
        async fn update_session_type(&self, arg0: SessionType) -> Result<Result24, AgentError>;
        async fn update_success_history(&self, arg0: SuccessHistoryItemV1) -> Result<Result24, AgentError>;
        async fn update_watch_history(&self, arg0: WatchHistoryItem) -> Result<Result24, AgentError>;
        async fn update_well_known_principal(
            &self,
            arg0: KnownPrincipalType,
            arg1: Principal,
        ) -> Result<(), AgentError>;
        async fn write_key_value_pair(
            &self,
            arg0: u64,
            arg1: String,
            arg2: String,
        ) -> Result<Result5, AgentError>;
        // async fn write_multiple_key_value_pairs(
        //     &self,
        //     arg0: u64,
        //     arg1: Vec<(String, String)>,
        // ) -> Result<Result6, AgentError>;
    }

    extern "Rust" {
        fn get_secp256k1_identity(jwk_key: JwkEcKey) -> Option<Secp256k1Identity>;
        fn get_jwk_ec_key(json_string: String) -> Option<JwkEcKey>;
    }

    extern "Rust" {
        type DelegatedIdentityWire;
        fn delegated_identity_from_bytes(data: &[u8]) -> Result<DelegatedIdentity, String>;
        fn delegated_identity_wire_from_bytes(data: &[u8]) -> Result<DelegatedIdentityWire, String>;
    }

    extern "Rust" {
        type CanistersWrapper;
        async fn authenticate_with_network(
            auth: DelegatedIdentityWire,
            referrer: Option<Principal>,
        ) -> Result<CanistersWrapper, String>;

        fn get_canister_principal(wrapper: CanistersWrapper) -> Principal;
        fn get_user_principal(wrapper: CanistersWrapper) -> Principal;
    }

    extern "Rust" {
        fn extract_time_as_double(result: Result11) -> Option<u64>;
    }
}

fn get_secp256k1_identity(jwk_key: JwkEcKey) -> Option<Secp256k1Identity> {
    match k256::SecretKey::from_jwk(&jwk_key) {
        Ok(key) => Some(Secp256k1Identity::from_private_key(key)),
        Err(_) => None,
    }
}

fn get_jwk_ec_key(json_string: String) -> Option<JwkEcKey> {
    let jwk_ec_key = JwkEcKey::from_str(&json_string);
    match jwk_ec_key {
        Ok(key) => Some(key),
        Err(_) => None,
    }    
}

pub trait FromBytes {
    fn from_bytes(data: &[u8]) -> std::result::Result<Self, String>
    where 
        Self: Sized;
}

impl FromBytes for DelegatedIdentityWire {
    fn from_bytes(data: &[u8]) -> std::result::Result<Self, String> {
        serde_json::from_slice(data).map_err(|e| e.to_string())
    }
}

fn delegated_identity_wire_from_bytes(data: &[u8]) -> std::result::Result<DelegatedIdentityWire, String> {
    return DelegatedIdentityWire::from_bytes(data).map_err( |e| e.to_string());
}

fn delegated_identity_from_bytes(data: &[u8]) -> std::result::Result<ic_agent::identity::DelegatedIdentity, String> {
    let wire = DelegatedIdentityWire::from_bytes(data)?;
    let to_secret = k256::SecretKey::from_jwk(&wire.to_secret)
        .map_err(|e| format!("Failed to parse secret key: {:?}", e))?;
    let to_identity = ic_agent::identity::Secp256k1Identity::from_private_key(to_secret);
    let delegated_identity = ic_agent::identity::DelegatedIdentity::new(
        wire.from_key,
        Box::new(to_identity),
        wire.delegation_chain,
    );
    Ok(delegated_identity)
}

pub struct CanistersWrapper {
    inner: Canisters<true>,
}

pub async fn authenticate_with_network(
    auth: DelegatedIdentityWire,
    referrer: Option<Principal>,
) -> std::result::Result<CanistersWrapper, String> {
    let canisters: Canisters<true> = Canisters::<true>::authenticate_with_network(auth, referrer).await.map_err( |error| error.to_string())?;
    Ok(CanistersWrapper { inner: canisters })
}

fn get_canister_principal(wrapper: CanistersWrapper) -> Principal {
    return wrapper.inner.user_canister()
}

fn get_user_principal(wrapper: CanistersWrapper) -> Principal {
    return wrapper.inner.user_principal()
}

pub fn extract_time_as_double(result: Result11) -> Option<u64> {
    match result {
        Result11::Ok(system_time) => Some(system_time.secs_since_epoch),
        Result11::Err(_) => None,
    }
}
