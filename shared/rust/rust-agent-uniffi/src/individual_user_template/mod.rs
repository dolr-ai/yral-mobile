#![allow(dead_code, unused_imports)]
mod individual_user_template_helper;

use candid::{self, CandidType, Decode, Deserialize, Encode, Nat, Principal};
use ic_agent::identity::Secp256k1Identity;
use ic_agent::{
    export::PrincipalError,
    identity::DelegatedIdentity,
    Agent,
};
use k256::elliptic_curve::JwkEcKey;
use std::sync::Arc;
use serde_bytes::ByteBuf;
use uniffi::Enum;
use uniffi::Record;
use crate::commons::*;
use crate::individual_user_template::individual_user_template_helper::*;
use crate::RUNTIME;
use crate::uni_ffi_helpers::*;

type Result<T> = std::result::Result<T, FFIError>;

#[derive(CandidType, Deserialize, Record)]
pub struct U64Wrapper {
    pub item: u64,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum PrincipalResult {
    Found(Principal),
    NotFound,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result_ {
    Ok(bool),
    Err(String),
}
#[derive(CandidType, Deserialize, Record)]
pub struct PostDetailsFromFrontend {
    pub is_nsfw: bool,
    pub hashtags: Vec<String>,
    pub description: String,
    pub video_uid: String,
    pub creator_consent_for_inclusion_in_hot_or_not: bool,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result1 {
    Ok(u64),
    Err(String),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum RejectionCode {
    NoError,
    CanisterError,
    SysTransient,
    DestinationInvalid,
    Unknown,
    SysFatal,
    CanisterReject,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum CdaoTokenError {
    NoBalance,
    InvalidRoot,
    CallError(RejectionCode, String),
    Transfer(TransferError),
    Unauthenticated,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result2 {
    Ok(bool),
    Err(CdaoTokenError),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum BetDirection {
    Hot,
    Not,
}
#[derive(CandidType, Deserialize, Record)]
pub struct PlaceBetArg {
    pub bet_amount: u64,
    pub post_id: u64,
    pub bet_direction: BetDirection,
    pub post_canister_id: Principal,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum BettingStatus {
    BettingOpen {
        number_of_participants: u8,
        ongoing_room: u64,
        ongoing_slot: u8,
        has_this_user_participated_in_this_post: Option<bool>,
        started_at: SystemTime,
    },
    BettingClosed,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum BetOnCurrentlyViewingPostError {
    UserPrincipalNotSet,
    InsufficientBalance,
    UserAlreadyParticipatedInThisPost,
    BettingClosed,
    Unauthorized,
    PostCreatorCanisterCallFailed,
    UserNotLoggedIn,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result3 {
    Ok(BettingStatus),
    Err(BetOnCurrentlyViewingPostError),
}
#[derive(CandidType, Deserialize, Record)]
pub struct NamespaceForFrontend {
    pub id: u64,
    pub title: String,
    pub owner_id: Principal,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum NamespaceErrors {
    UserNotSignedUp,
    ValueTooBig,
    NamespaceNotFound,
    Unauthorized,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result4 {
    Ok(NamespaceForFrontend),
    Err(NamespaceErrors),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result5 {
    Ok(Option<String>),
    Err(NamespaceErrors),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result6 {
    Ok,
    Err(NamespaceErrors),
}
#[derive(CandidType, Deserialize, Record)]
pub struct Canister {
    pub id: Option<Principal>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct DappCanisters {
    pub canisters: Vec<Canister>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct NeuronsFundParticipants {
    pub participants: Vec<CfParticipant>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct TreasuryDistribution {
    pub total_e8s: u64,
}
#[derive(CandidType, Deserialize, Record)]
pub struct NeuronDistribution {
    pub controller: Option<Principal>,
    pub dissolve_delay_seconds: u64,
    pub memo: u64,
    pub stake_e8s: u64,
    pub vesting_period_seconds: Option<u64>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct DeveloperDistribution {
    pub developer_neurons: Vec<NeuronDistribution>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct AirdropDistribution {
    pub airdrop_neurons: Vec<NeuronDistribution>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct SwapDistribution {
    pub total_e8s: u64,
    pub initial_swap_amount_e8s: u64,
}
#[derive(CandidType, Deserialize, Record)]
pub struct MFractionalDeveloperVotingPower {
    pub treasury_distribution: Option<TreasuryDistribution>,
    pub developer_distribution: Option<DeveloperDistribution>,
    pub airdrop_distribution: Option<AirdropDistribution>,
    pub swap_distribution: Option<SwapDistribution>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum InitialTokenDistribution {
    FractionalDeveloperVotingPower(MFractionalDeveloperVotingPower),
}
#[derive(CandidType, Deserialize, Record)]
pub struct SnsInitPayload {
    pub url: Option<String>,
    pub max_dissolve_delay_seconds: Option<u64>,
    pub max_dissolve_delay_bonus_percentage: Option<u64>,
    pub nns_proposal_id: Option<u64>,
    pub neurons_fund_participation: Option<bool>,
    pub min_participant_icp_e8s: Option<u64>,
    pub neuron_basket_construction_parameters: Option<NeuronBasketConstructionParameters>,
    pub fallback_controller_principal_ids: Vec<String>,
    pub token_symbol: Option<String>,
    pub final_reward_rate_basis_points: Option<u64>,
    pub max_icp_e8s: Option<u64>,
    pub neuron_minimum_stake_e8s: Option<u64>,
    pub confirmation_text: Option<String>,
    pub logo: Option<String>,
    pub name: Option<String>,
    pub swap_start_timestamp_seconds: Option<u64>,
    pub swap_due_timestamp_seconds: Option<u64>,
    pub initial_voting_period_seconds: Option<u64>,
    pub neuron_minimum_dissolve_delay_to_vote_seconds: Option<u64>,
    pub description: Option<String>,
    pub max_neuron_age_seconds_for_age_bonus: Option<u64>,
    pub min_participants: Option<u64>,
    pub initial_reward_rate_basis_points: Option<u64>,
    pub wait_for_quiet_deadline_increase_seconds: Option<u64>,
    pub transaction_fee_e8s: Option<u64>,
    pub dapp_canisters: Option<DappCanisters>,
    pub neurons_fund_participation_constraints: Option<NeuronsFundParticipationConstraints>,
    pub neurons_fund_participants: Option<NeuronsFundParticipants>,
    pub max_age_bonus_percentage: Option<u64>,
    pub initial_token_distribution: Option<InitialTokenDistribution>,
    pub reward_rate_transition_duration_seconds: Option<u64>,
    pub token_logo: Option<String>,
    pub token_name: Option<String>,
    pub max_participant_icp_e8s: Option<u64>,
    pub min_direct_participation_icp_e8s: Option<u64>,
    pub proposal_reject_cost_e8s: Option<u64>,
    pub restricted_countries: Option<Countries>,
    pub min_icp_e8s: Option<u64>,
    pub max_direct_participation_icp_e8s: Option<u64>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct DeployedCdaoCanisters {
    pub root: Principal,
    pub swap: Principal,
    pub ledger: Principal,
    pub index: Principal,
    pub governance: Principal,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum CdaoDeployError {
    CycleError(String),
    Unregistered,
    CallError(RejectionCode, String),
    InvalidInitPayload(String),
    TokenLimit(u64),
    Unauthenticated,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result7 {
    Ok(DeployedCdaoCanisters),
    Err(CdaoDeployError),
}
#[derive(CandidType, Deserialize, Record)]
pub struct FolloweeArg {
    pub followee_canister_id: Principal,
    pub followee_principal_id: Principal,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum FollowAnotherUserProfileError {
    UserITriedToFollowCrossCanisterCallFailed,
    UsersICanFollowListIsFull,
    Unauthorized,
    UserITriedToFollowHasTheirFollowersListFull,
    Unauthenticated,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result8 {
    Ok(bool),
    Err(FollowAnotherUserProfileError),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum BetMakerInformedStatus {
    InformedSuccessfully,
    Failed(String),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum BetPayout {
    NotCalculatedYet,
    Calculated(u64),
}
#[derive(CandidType, Deserialize, Record)]
pub struct BetDetails {
    pub bet_direction: BetDirection,
    pub bet_maker_canister_id: Principal,
    pub bet_maker_informed_status: Option<BetMakerInformedStatus>,
    pub amount: u64,
    pub payout: BetPayout,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result9 {
    Ok(BetDetails),
    Err(String),
}
#[derive(CandidType, Deserialize, Record)]
pub struct DeviceIdentity {
    pub device_id: String,
    pub timestamp: u64,
}
#[derive(CandidType, Deserialize, Record)]
pub struct FeedScore {
    pub current_score: u64,
    pub last_synchronized_at: SystemTime,
    pub last_synchronized_score: u64,
}
#[derive(CandidType, Deserialize, Record)]
pub struct PostViewStatistics {
    pub total_view_count: u64,
    pub average_watch_percentage: u8,
    pub threshold_view_count: u64,
}
#[derive(CandidType, Deserialize, Record)]
pub struct AggregateStats {
    pub total_number_of_not_bets: u64,
    pub total_amount_bet: u64,
    pub total_number_of_hot_bets: u64,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum RoomBetPossibleOutcomes {
    HotWon,
    BetOngoing,
    Draw,
    NotWon,
}
#[derive(CandidType, Deserialize, Record)]
pub struct RoomDetails {
    pub total_hot_bets: u64,
    pub bets_made: Vec<PrincipalBetDetailsPair>,
    pub total_not_bets: u64,
    pub room_bets_total_pot: u64,
    pub bet_outcome: RoomBetPossibleOutcomes,
}
#[derive(CandidType, Deserialize, Record)]
pub struct SlotDetails {
    pub room_details: Vec<NumberRoomDetailsPair>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct HotOrNotDetails {
    pub hot_or_not_feed_score: FeedScore,
    pub aggregate_stats: AggregateStats,
    pub slot_history: Vec<NumberSlotDetailsPair>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct Post {
    pub id: u64,
    pub is_nsfw: bool,
    pub status: PostStatus,
    pub share_count: u64,
    pub hashtags: Vec<String>,
    pub description: String,
    pub created_at: SystemTime,
    pub likes: Vec<Principal>,
    pub video_uid: String,
    pub home_feed_score: FeedScore,
    pub slots_left_to_be_computed: ByteBuf,
    pub view_stats: PostViewStatistics,
    pub hot_or_not_details: Option<HotOrNotDetails>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result10 {
    Ok(Post),
    Err,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum BetOutcomeForBetMaker {
    Won(u64),
    Draw(u64),
    Lost,
    AwaitingResult,
}
#[derive(CandidType, Deserialize, Record)]
pub struct PlacedBetDetail {
    pub outcome_received: BetOutcomeForBetMaker,
    pub slot_id: u8,
    pub post_id: u64,
    pub room_id: u64,
    pub canister_id: Principal,
    pub bet_direction: BetDirection,
    pub amount_bet: u64,
    pub bet_placed_at: SystemTime,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum PlacedBetDetailResult {
    Found(PlacedBetDetail),
    NotFound,
}
#[derive(CandidType, Deserialize, Record)]
pub struct PostDetailsForFrontend {
    pub id: u64,
    pub is_nsfw: bool,
    pub status: PostStatus,
    pub home_feed_ranking_score: u64,
    pub hashtags: Vec<String>,
    pub hot_or_not_betting_status: Option<BettingStatus>,
    pub like_count: u64,
    pub description: String,
    pub total_view_count: u64,
    pub created_by_display_name: Option<String>,
    pub created_at: SystemTime,
    pub created_by_unique_user_name: Option<String>,
    pub video_uid: String,
    pub created_by_user_principal_id: Principal,
    pub hot_or_not_feed_ranking_score: Option<u64>,
    pub liked_by_me: bool,
    pub created_by_profile_photo_url: Option<String>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result11 {
    Ok(SystemTime),
    Err(String),
}
#[derive(CandidType, Deserialize, Record)]
pub struct MlFeedCacheItem {
    pub post_id: u64,
    pub canister_id: Principal,
    pub video_id: String,
    pub creator_principal_id: Option<Principal>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum GetPostsOfUserProfileError {
    ReachedEndOfItemsList,
    InvalidBoundsPassed,
    ExceededMaxNumberOfItemsAllowedInOneRequest,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result12 {
    Ok(Vec<PostDetailsForFrontend>),
    Err(GetPostsOfUserProfileError),
}
#[derive(CandidType, Deserialize, Record)]
pub struct FollowEntryDetail {
    pub canister_id: Principal,
    pub principal_id: Principal,
}
#[derive(CandidType, Deserialize, Record)]
pub struct UserProfileGlobalStats {
    pub hot_bets_received: u64,
    pub not_bets_received: u64,
}
#[derive(CandidType, Deserialize, Record)]
pub struct UserCanisterDetails {
    pub user_canister_id: Principal,
    pub profile_owner: Principal,
}
#[derive(CandidType, Deserialize, Record)]
pub struct UserProfileDetailsForFrontend {
    pub unique_user_name: Option<String>,
    pub lifetime_earnings: u64,
    pub following_count: u64,
    pub profile_picture_url: Option<String>,
    pub display_name: Option<String>,
    pub principal_id: Principal,
    pub profile_stats: UserProfileGlobalStats,
    pub followers_count: u64,
    pub referrer_details: Option<UserCanisterDetails>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum MigrationInfo {
    MigratedFromHotOrNot { account_principal: Principal },
    NotMigrated,
    MigratedToYral { account_principal: Principal },
}
#[derive(CandidType, Deserialize, Record)]
pub struct UserProfileDetailsForFrontendV2 {
    pub unique_user_name: Option<String>,
    pub lifetime_earnings: u64,
    pub migration_info: MigrationInfo,
    pub following_count: u64,
    pub profile_picture_url: Option<String>,
    pub display_name: Option<String>,
    pub principal_id: Principal,
    pub profile_stats: UserProfileGlobalStats,
    pub followers_count: u64,
    pub referrer_details: Option<UserCanisterDetails>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum SessionType {
    AnonymousSession,
    RegisteredSession,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result13 {
    Ok(SessionType),
    Err(String),
}
#[derive(CandidType, Deserialize, Record)]
pub struct SuccessHistoryItemV1 {
    pub post_id: u64,
    pub percentage_watched: f32,
    pub item_type: String,
    pub publisher_canister_id: Principal,
    pub cf_video_id: String,
    pub interacted_at: SystemTime,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result14 {
    Ok(Vec<SuccessHistoryItemV1>),
    Err(String),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum PaginationError {
    ReachedEndOfItemsList,
    InvalidBoundsPassed,
    ExceededMaxNumberOfItemsAllowedInOneRequest,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result15 {
    Ok(Vec<Principal>),
    Err(PaginationError),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum StakeEvent {
    BetOnHotOrNotPost(PlaceBetArg),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum MintEvent {
    NewUserSignup {
        new_user_principal_id: Principal,
    },
    Referral {
        referrer_user_principal_id: Principal,
        referee_user_principal_id: Principal,
    },
}
#[derive(CandidType, Deserialize, Enum)]
pub enum HotOrNotOutcomePayoutEvent {
    WinningsEarnedFromBet {
        slot_id: u8,
        post_id: u64,
        room_id: u64,
        post_canister_id: Principal,
        winnings_amount: u64,
        event_outcome: BetOutcomeForBetMaker,
    },
    CommissionFromHotOrNotBet {
        slot_id: u8,
        post_id: u64,
        room_pot_total_amount: u64,
        room_id: u64,
        post_canister_id: Principal,
    },
}
#[derive(CandidType, Deserialize, Enum)]
pub enum TokenEvent {
    Stake {
        timestamp: SystemTime,
        details: StakeEvent,
        amount: u64,
    },
    Burn,
    Mint {
        timestamp: SystemTime,
        details: MintEvent,
        amount: u64,
    },
    Transfer {
        to_account: Principal,
        timestamp: SystemTime,
        amount: u64,
    },
    HotOrNotOutcomePayout {
        timestamp: SystemTime,
        details: HotOrNotOutcomePayoutEvent,
        amount: u64,
    },
    Receive {
        from_account: Principal,
        timestamp: SystemTime,
        amount: u64,
    },
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result16 {
    Ok(Vec<NumberTokenEventPair>),
    Err(PaginationError),
}
#[derive(CandidType, Deserialize, Record)]
pub struct WatchHistoryItem {
    pub post_id: u64,
    pub viewed_at: SystemTime,
    pub percentage_watched: f32,
    pub publisher_canister_id: Principal,
    pub cf_video_id: String,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result17 {
    Ok(Vec<WatchHistoryItem>),
    Err(String),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result18 {
    Ok(Vec<String>),
    Err(NamespaceErrors),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result19 {
    Ok(Vec<IntBytePair>),
    Err(String),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum MigrationErrors {
    InvalidToCanister,
    InvalidFromCanister,
    MigrationInfoNotFound,
    UserNotRegistered,
    RequestCycleFromUserIndexFailed(String),
    UserIndexCanisterIdNotFound,
    Unauthorized,
    TransferToCanisterCallFailed(String),
    HotOrNotSubnetCanisterIdNotFound,
    AlreadyUsedForMigration,
    CanisterInfoFailed,
    AlreadyMigrated,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result20 {
    Ok,
    Err(MigrationErrors),
}
#[derive(CandidType, Deserialize, Record)]
pub struct FundCommitted {
    pub total_direct_participation_icp_e8s: Option<u64>,
    pub total_neurons_fund_participation_icp_e8s: Option<u64>,
    pub sns_governance_canister_id: Option<Principal>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result21 {
    Committed(FundCommitted),
    Aborted {},
}
#[derive(CandidType, Deserialize, Record)]
pub struct SettleNeuronsFundParticipationRequest {
    pub result: Option<Result21>,
    pub nns_proposal_id: Option<u64>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct NeuronsFundNeuron {
    pub hotkey_principal: Option<String>,
    pub is_capped: Option<bool>,
    pub nns_neuron_id: Option<u64>,
    pub amount_icp_e8s: Option<u64>,
}
#[derive(CandidType, Deserialize, Record)]
pub struct NeuronsOk {
    pub neurons_fund_neuron_portions: Vec<NeuronsFundNeuron>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result22 {
    Ok(NeuronsOk),
    Err(GovernanceError),
}
#[derive(CandidType, Deserialize, Record)]
pub struct SettleNeuronsFundParticipationResponse {
    pub result: Option<Result22>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result23 {
    Ok,
    Err(CdaoTokenError),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result24 {
    Ok(String),
    Err(String),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum PostViewDetailsFromFrontend {
    WatchedMultipleTimes {
        percentage_watched: u8,
        watch_count: u8,
    },
    WatchedPartially {
        percentage_watched: u8,
    },
}
#[derive(CandidType, Deserialize, Record)]
pub struct UserProfileUpdateDetailsFromFrontend {
    pub profile_picture_url: Option<String>,
    pub display_name: Option<String>,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum UpdateProfileDetailsError {
    NotAuthorized,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result25 {
    Ok(UserProfileDetailsForFrontend),
    Err(UpdateProfileDetailsError),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result26 {
    Ok,
    Err(String),
}
#[derive(CandidType, Deserialize, Enum)]
pub enum UpdateProfileSetUniqueUsernameError {
    UsernameAlreadyTaken,
    UserIndexCrossCanisterCallFailed,
    SendingCanisterDoesNotMatchUserCanisterId,
    NotAuthorized,
    UserCanisterEntryDoesNotExist,
}
#[derive(CandidType, Deserialize, Enum)]
pub enum Result27 {
    Ok,
    Err(UpdateProfileSetUniqueUsernameError),
}
#[derive(CandidType, Deserialize, Record)]
pub struct FollowerArg {
    pub follower_canister_id: Principal,
    pub follower_principal_id: Principal,
}
#[derive(Deserialize, CandidType, Record)]
pub struct FollowEntry {
    pub id: u64,
    pub detail: FollowEntryDetail,
}
#[derive(CandidType, Deserialize, Record)]
pub struct NumberTokenEventPair {
    pub first: u64,
    pub second: TokenEvent,
}
#[derive(CandidType, Deserialize, Record)]
pub struct NumberSlotDetailsPair {
    pub first: u8,
    pub second: SlotDetails,
}
#[derive(CandidType, Deserialize, Record)]
pub struct NumberRoomDetailsPair {
    pub first: u64,
    pub second: RoomDetails,
}
#[derive(CandidType, Deserialize, Record)]
pub struct PrincipalBetDetailsPair {
    pub first: Principal,
    pub second: BetDetails,
}

#[derive(uniffi::Object)]
pub struct IndividualUserService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

#[uniffi::export]
impl IndividualUserService {
    #[uniffi::constructor]
    pub fn new(
        principal_text: &str,
        identity_data: Vec<u8>,
    ) -> std::result::Result<IndividualUserService, FFIError> {
        let principal = Principal::from_text(principal_text)
            .map_err(|e| FFIError::PrincipalError(format!("Invalid principal: {:?}", e)))?;
        let identity = delegated_identity_from_bytes(&identity_data.as_slice())
            .map_err(|e| FFIError::UnknownError(format!("Invalid: {:?}", e)))?;
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .with_identity(identity)
            .build()
            .map_err(|e| FFIError::AgentError(format!("Failed to create agent: {:?}", e)))?;

        Ok(IndividualUserService {
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
    pub async fn add_device_id(&self, arg0: String) -> Result<Result_> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("add_device_id", args).await?;
        Ok(Decode!(&bytes, Result_)?)
    }
    #[uniffi::method]
    pub async fn add_post_v_2(&self, arg0: PostDetailsFromFrontend) -> Result<Result1> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("add_post_v2", args).await?;
        Ok(Decode!(&bytes, Result1)?)
    }
    #[uniffi::method]
    pub async fn add_token(&self, arg0: Principal) -> Result<Result2> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("add_token", args).await?;
        Ok(Decode!(&bytes, Result2)?)
    }
    #[uniffi::method]
    pub async fn bet_on_currently_viewing_post(&self, arg0: PlaceBetArg) -> Result<Result3> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("bet_on_currently_viewing_post", args).await?;
        Ok(Decode!(&bytes, Result3)?)
    }
    #[uniffi::method]
    pub async fn check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold(
        &self,
        arg0: Vec<u64>,
    ) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("check_and_update_scores_and_share_with_post_cache_if_difference_beyond_threshold", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn clear_snapshot(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self.update_canister("clear_snapshot", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn create_a_namespace(&self, arg0: String) -> Result<Result4> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("create_a_namespace", args).await?;
        Ok(Decode!(&bytes, Result4)?)
    }
    #[uniffi::method]
    pub async fn delete_key_value_pair(&self, arg0: u64, arg1: String) -> Result<Result5> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("delete_key_value_pair", args).await?;
        Ok(Decode!(&bytes, Result5)?)
    }
    #[uniffi::method]
    pub async fn delete_multiple_key_value_pairs(
        &self,
        arg0: u64,
        arg1: Vec<String>,
    ) -> Result<Result6> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("delete_multiple_key_value_pairs", args).await?;
        Ok(Decode!(&bytes, Result6)?)
    }
    #[uniffi::method]
    pub async fn deploy_cdao_sns(&self, arg0: SnsInitPayload, arg1: u64) -> Result<Result7> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("deploy_cdao_sns", args).await?;
        Ok(Decode!(&bytes, Result7)?)
    }
    #[uniffi::method]
    pub async fn deployed_cdao_canisters(&self) -> Result<Vec<DeployedCdaoCanisters>> {
        let args = Encode!()?;
        let bytes = self.query_canister("deployed_cdao_canisters", args).await?;
        Ok(Decode!(&bytes, Vec<DeployedCdaoCanisters>)?)
    }
    #[uniffi::method]
    pub async fn do_i_follow_this_user(&self, arg0: FolloweeArg) -> Result<Result8> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("do_i_follow_this_user", args).await?;
        Ok(Decode!(&bytes, Result8)?)
    }
    #[uniffi::method]
    pub async fn download_snapshot(&self, arg0: u64, arg1: u64) -> Result<ByteBuf> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("download_snapshot", args).await?;
        Ok(Decode!(&bytes, ByteBuf)?)
    }
    #[uniffi::method]
    pub async fn get_bet_details_for_a_user_on_a_post(
        &self,
        arg0: Principal,
        arg1: u64,
    ) -> Result<Result9> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_bet_details_for_a_user_on_a_post", args).await?;
        Ok(Decode!(&bytes, Result9)?)
    }
    #[uniffi::method]
    pub async fn get_device_identities(&self) -> Result<Vec<DeviceIdentity>> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_device_identities", args).await?;
        Ok(Decode!(&bytes, Vec<DeviceIdentity>)?)
    }
    #[uniffi::method]
    pub async fn get_entire_individual_post_detail_by_id(&self, arg0: u64) -> Result<Result10> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_entire_individual_post_detail_by_id", args).await?;
        Ok(Decode!(&bytes, Result10)?)
    }
    #[uniffi::method]
    pub async fn get_hot_or_not_bet_details_for_this_post(
        &self,
        arg0: u64,
    ) -> Result<BettingStatus> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_hot_or_not_bet_details_for_this_post", args).await?;
        Ok(Decode!(&bytes, BettingStatus)?)
    }
    #[uniffi::method]
    pub async fn get_hot_or_not_bets_placed_by_this_profile_with_pagination(
        &self,
        arg0: u64,
    ) -> Result<Vec<PlacedBetDetail>> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_hot_or_not_bets_placed_by_this_profile_with_pagination", args).await?;
        Ok(Decode!(&bytes, Vec<PlacedBetDetail>)?)
    }
    #[uniffi::method]
    pub async fn get_individual_hot_or_not_bet_placed_by_this_profile(
        &self,
        arg0: Principal,
        arg1: u64,
    ) -> Result<PlacedBetDetailResult> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_individual_hot_or_not_bet_placed_by_this_profile", args).await?;
        let result = Decode!(&bytes, Option<PlacedBetDetail>)?;
        match result {
            Some(placed_bet_details) => {
                Ok(PlacedBetDetailResult::Found(placed_bet_details))
            }
            None => Ok(PlacedBetDetailResult::NotFound),
        }
    }
    #[uniffi::method]
    pub async fn get_individual_post_details_by_id(
        &self,
        arg0: u64,
    ) -> Result<PostDetailsForFrontend> {
        let args = Encode!(&arg0)?;
        let call_result = self
            .agent
            .query(&self.principal, "get_individual_post_details_by_id")
            .with_arg(args)
            .call()
            .await;
        match call_result {
            Ok(bytes) => {
                // Decode the bytes if the call succeeded
                match Decode!(&bytes, PostDetailsForFrontend) {
                    Ok(details) => Ok(details),
                    Err(e) => {
                        eprintln!("Failed to decode PostDetailsForFrontend: {:?}", e);
                        Err(e.into())
                    }
                }
            }
            Err(e) => {
                // Log the error here
                eprintln!("Error calling get_individual_post_details_by_id: {:?}", e);
                Err(e.into())
            }
        }
    }
    #[uniffi::method]
    pub async fn get_last_access_time(&self) -> Result<Result11> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_last_access_time", args).await?;
        Ok(Decode!(&bytes, Result11)?)
    }
    #[uniffi::method]
    pub async fn get_last_canister_functionality_access_time(&self) -> Result<Result11> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_last_canister_functionality_access_time", args).await?;
        Ok(Decode!(&bytes, Result11)?)
    }
    #[uniffi::method]
    pub async fn get_ml_feed_cache_paginated(
        &self,
        arg0: u64,
        arg1: u64,
    ) -> Result<Vec<MlFeedCacheItem>> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_ml_feed_cache_paginated", args).await?;
        Ok(Decode!(&bytes, Vec<MlFeedCacheItem>)?)
    }
    #[uniffi::method]
    pub async fn get_posts_of_this_user_profile_with_pagination(
        &self,
        arg0: u64,
        arg1: u64,
    ) -> Result<Result12> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_posts_of_this_user_profile_with_pagination", args).await?;
        Ok(Decode!(&bytes, Result12)?)
    }
    #[uniffi::method]
    pub async fn get_posts_of_this_user_profile_with_pagination_cursor(
        &self,
        arg0: u64,
        arg1: u64,
    ) -> Result<Result12> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_posts_of_this_user_profile_with_pagination_cursor", args).await?;
        Ok(Decode!(&bytes, Result12)?)
    }
    #[uniffi::method]
    pub async fn get_principals_that_follow_this_profile_paginated(
        &self,
        arg0: Option<u64>,
    ) -> Result<Vec<FollowEntry>> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_principals_that_follow_this_profile_paginated", args).await?;
        Ok(Decode!(&bytes, Vec<FollowEntry>)?)
    }
    #[uniffi::method]
    pub async fn get_principals_this_profile_follows_paginated(
        &self,
        arg0: Option<u64>,
    ) -> Result<Vec<FollowEntry>> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_principals_this_profile_follows_paginated", args).await?;
        Ok(Decode!(&bytes, Vec<FollowEntry>)?)
    }
    #[uniffi::method]
    pub async fn get_profile_details(&self) -> Result<UserProfileDetailsForFrontend> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_profile_details", args).await?;
        Ok(Decode!(&bytes, UserProfileDetailsForFrontend)?)
    }
    #[uniffi::method]
    pub async fn get_profile_details_v_2(&self) -> Result<UserProfileDetailsForFrontendV2> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_profile_details_v2", args).await?;
        Ok(Decode!(&bytes, UserProfileDetailsForFrontendV2)?)
    }
    #[uniffi::method]
    pub async fn get_rewarded_for_referral(&self, arg0: Principal, arg1: Principal) -> Result<()> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("get_rewarded_for_referral", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn get_rewarded_for_signing_up(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self.update_canister("get_rewarded_for_signing_up", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn get_session_type(&self) -> Result<Result13> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_session_type", args).await?;
        Ok(Decode!(&bytes, Result13)?)
    }
    #[uniffi::method]
    pub async fn get_stable_memory_size(&self) -> Result<U64Wrapper> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_stable_memory_size", args).await?;
        Ok(Decode!(&bytes, U64Wrapper)?)
    }
    #[uniffi::method]
    pub async fn get_success_history(&self) -> Result<Result14> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_success_history", args).await?;
        Ok(Decode!(&bytes, Result14)?)
    }
    #[uniffi::method]
    pub async fn get_token_roots_of_this_user_with_pagination_cursor(
        &self,
        arg0: u64,
        arg1: u64,
    ) -> Result<Result15> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_token_roots_of_this_user_with_pagination_cursor", args).await?;
        Ok(Decode!(&bytes, Result15)?)
    }
    #[uniffi::method]
    pub async fn get_user_caniser_cycle_balance(&self) -> Result<Nat> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_user_caniser_cycle_balance", args).await?;
        Ok(Decode!(&bytes, Nat)?)
    }
    #[uniffi::method]
    pub async fn get_user_utility_token_transaction_history_with_pagination(
        &self,
        arg0: u64,
        arg1: u64,
    ) -> Result<Result16> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("get_user_utility_token_transaction_history_with_pagination", args).await?;
        Ok(Decode!(&bytes, Result16)?)
    }
    #[uniffi::method]
    pub async fn get_utility_token_balance(&self) -> Result<U64Wrapper> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_utility_token_balance", args).await?;
        Ok(Decode!(&bytes, U64Wrapper)?)
    }
    #[uniffi::method]
    pub async fn get_version(&self) -> Result<String> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_version", args).await?;
        Ok(Decode!(&bytes, String)?)
    }
    #[uniffi::method]
    pub async fn get_version_number(&self) -> Result<U64Wrapper> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_version_number", args).await?;
        Ok(Decode!(&bytes, U64Wrapper)?)
    }
    #[uniffi::method]
    pub async fn get_watch_history(&self) -> Result<Result17> {
        let args = Encode!()?;
        let bytes = self.query_canister("get_watch_history", args).await?;
        Ok(Decode!(&bytes, Result17)?)
    }
    #[uniffi::method]
    pub async fn get_well_known_principal_value(
        &self,
        arg0: KnownPrincipalType,
    ) -> Result<PrincipalResult> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("get_well_known_principal_value", args).await?;
        let result = Decode!(&bytes, Option<Principal>)?;
        match result {
            Some(principal) => Ok(PrincipalResult::Found(principal)),
            None => Ok(PrincipalResult::NotFound),
        }
    }
    #[uniffi::method]
    pub async fn http_request(&self, arg0: HttpRequest) -> Result<HttpResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("http_request", args).await?;
        Ok(Decode!(&bytes, HttpResponse)?)
    }
    #[uniffi::method]
    pub async fn list_namespace_keys(&self, arg0: u64) -> Result<Result18> {
        let args = Encode!(&arg0)?;
        let bytes = self.query_canister("list_namespace_keys", args).await?;
        Ok(Decode!(&bytes, Result18)?)
    }
    #[uniffi::method]
    pub async fn list_namespaces(&self, arg0: u64, arg1: u64) -> Result<Vec<NamespaceForFrontend>> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("list_namespaces", args).await?;
        Ok(Decode!(&bytes, Vec<NamespaceForFrontend>)?)
    }
    #[uniffi::method]
    pub async fn load_snapshot(&self, arg0: u64) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("load_snapshot", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn once_reenqueue_timers_for_pending_bet_outcomes(&self) -> Result<Result19> {
        let args = Encode!()?;
        let bytes = self.update_canister("once_reenqueue_timers_for_pending_bet_outcomes", args).await?;
        Ok(Decode!(&bytes, Result19)?)
    }
    #[uniffi::method]
    pub async fn read_key_value_pair(&self, arg0: u64, arg1: String) -> Result<Result5> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.query_canister("read_key_value_pair", args).await?;
        Ok(Decode!(&bytes, Result5)?)
    }
    #[uniffi::method]
    pub async fn receive_and_save_snaphot(
        &self,
        arg0: u64,
        arg1: ByteBuf,
    ) -> Result<()> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("receive_and_save_snaphot", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn receive_bet_from_bet_makers_canister(
        &self,
        arg0: PlaceBetArg,
        arg1: Principal,
    ) -> Result<Result3> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("receive_bet_from_bet_makers_canister", args).await?;
        Ok(Decode!(&bytes, Result3)?)
    }
    #[uniffi::method]
    pub async fn receive_bet_winnings_when_distributed(
        &self,
        arg0: u64,
        arg1: BetOutcomeForBetMaker,
    ) -> Result<()> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("receive_bet_winnings_when_distributed", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn receive_data_from_hotornot(
        &self,
        arg0: Principal,
        arg1: u64,
        arg2: Vec<Post>,
    ) -> Result<Result20> {
        let args = Encode!(&arg0, &arg1, &arg2)?;
        let bytes = self.update_canister("receive_data_from_hotornot", args).await?;
        Ok(Decode!(&bytes, Result20)?)
    }
    #[uniffi::method]
    pub async fn return_cycles_to_user_index_canister(
        &self,
        arg0: Option<Nat>,
    ) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("return_cycles_to_user_index_canister", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn save_snapshot_json(&self) -> Result<u32> {
        let args = Encode!()?;
        let bytes = self.update_canister("save_snapshot_json", args).await?;
        Ok(Decode!(&bytes, u32)?)
    }
    #[uniffi::method]
    pub async fn settle_neurons_fund_participation(
        &self,
        arg0: SettleNeuronsFundParticipationRequest,
    ) -> Result<SettleNeuronsFundParticipationResponse> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("settle_neurons_fund_participation", args).await?;
        Ok(Decode!(&bytes, SettleNeuronsFundParticipationResponse)?)
    }
    #[uniffi::method]
    pub async fn transfer_token_to_user_canister(
        &self,
        arg0: Principal,
        arg1: Principal,
        arg2: Option<ByteBuf>,
        arg3: Nat,
    ) -> Result<Result23> {
        let args = Encode!(&arg0, &arg1, &arg2, &arg3)?;
        let bytes = self.update_canister("transfer_token_to_user_canister", args).await?;
        Ok(Decode!(&bytes, Result23)?)
    }
    #[uniffi::method]
    pub async fn transfer_tokens_and_posts(
        &self,
        arg0: Principal,
        arg1: Principal,
    ) -> Result<Result20> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("transfer_tokens_and_posts", args).await?;
        Ok(Decode!(&bytes, Result20)?)
    }
    #[uniffi::method]
    pub async fn update_last_access_time(&self) -> Result<Result24> {
        let args = Encode!()?;
        let bytes = self.update_canister("update_last_access_time", args).await?;
        Ok(Decode!(&bytes, Result24)?)
    }
    #[uniffi::method]
    pub async fn update_last_canister_functionality_access_time(&self) -> Result<()> {
        let args = Encode!()?;
        let bytes = self.update_canister("update_last_canister_functionality_access_time", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn update_ml_feed_cache(&self, arg0: Vec<MlFeedCacheItem>) -> Result<Result24> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_ml_feed_cache", args).await?;
        Ok(Decode!(&bytes, Result24)?)
    }
    #[uniffi::method]
    pub async fn update_post_add_view_details(
        &self,
        arg0: u64,
        arg1: PostViewDetailsFromFrontend,
    ) -> Result<()> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("update_post_add_view_details", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn update_post_as_ready_to_view(&self, arg0: u64) -> Result<()> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_post_as_ready_to_view", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn update_post_increment_share_count(&self, arg0: u64) -> Result<U64Wrapper> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_post_increment_share_count", args).await?;
        Ok(Decode!(&bytes, U64Wrapper)?)
    }
    #[uniffi::method]
    pub async fn update_post_status(&self, arg0: u64, arg1: PostStatus) -> Result<()> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("update_post_status", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn update_post_toggle_like_status_by_caller(&self, arg0: u64) -> Result<bool> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_post_toggle_like_status_by_caller", args).await?;
        Ok(Decode!(&bytes, bool)?)
    }
    #[uniffi::method]
    pub async fn update_profile_display_details(
        &self,
        arg0: UserProfileUpdateDetailsFromFrontend,
    ) -> Result<Result25> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_profile_display_details", args).await?;
        Ok(Decode!(&bytes, Result25)?)
    }
    #[uniffi::method]
    pub async fn update_profile_owner(&self, arg0: Option<Principal>) -> Result<Result26> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_profile_owner", args).await?;
        Ok(Decode!(&bytes, Result26)?)
    }
    #[uniffi::method]
    pub async fn update_profile_set_unique_username_once(&self, arg0: String) -> Result<Result27> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_profile_set_unique_username_once", args).await?;
        Ok(Decode!(&bytes, Result27)?)
    }
    #[uniffi::method]
    pub async fn update_profiles_i_follow_toggle_list_with_specified_profile(
        &self,
        arg0: FolloweeArg,
    ) -> Result<Result8> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_profiles_i_follow_toggle_list_with_specified_profile", args).await?;
        Ok(Decode!(&bytes, Result8)?)
    }
    #[uniffi::method]
    pub async fn update_profiles_that_follow_me_toggle_list_with_specified_profile(
        &self,
        arg0: FollowerArg,
    ) -> Result<Result8> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_profiles_that_follow_me_toggle_list_with_specified_profile", args).await?;
        Ok(Decode!(&bytes, Result8)?)
    }
    #[uniffi::method]
    pub async fn update_referrer_details(&self, arg0: UserCanisterDetails) -> Result<Result24> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_referrer_details", args).await?;
        Ok(Decode!(&bytes, Result24)?)
    }
    #[uniffi::method]
    pub async fn update_session_type(&self, arg0: SessionType) -> Result<Result24> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_session_type", args).await?;
        Ok(Decode!(&bytes, Result24)?)
    }
    #[uniffi::method]
    pub async fn update_success_history(&self, arg0: SuccessHistoryItemV1) -> Result<Result24> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_success_history", args).await?;
        Ok(Decode!(&bytes, Result24)?)
    }
    #[uniffi::method]
    pub async fn update_watch_history(&self, arg0: WatchHistoryItem) -> Result<Result24> {
        let args = Encode!(&arg0)?;
        let bytes = self.update_canister("update_watch_history", args).await?;
        Ok(Decode!(&bytes, Result24)?)
    }
    #[uniffi::method]
    pub async fn update_well_known_principal(
        &self,
        arg0: KnownPrincipalType,
        arg1: Principal,
    ) -> Result<()> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("update_well_known_principal", args).await?;
        Ok(Decode!(&bytes)?)
    }
    #[uniffi::method]
    pub async fn write_key_value_pair(
        &self,
        arg0: u64,
        arg1: String,
        arg2: String,
    ) -> Result<Result5> {
        let args = Encode!(&arg0, &arg1, &arg2)?;
        let bytes = self.update_canister("write_key_value_pair", args).await?;
        Ok(Decode!(&bytes, Result5)?)
    }
    #[uniffi::method]
    pub async fn write_multiple_key_value_pairs(
        &self,
        arg0: u64,
        arg1: Vec<KeyValuePair>,
    ) -> Result<Result6> {
        let args = Encode!(&arg0, &arg1)?;
        let bytes = self.update_canister("write_multiple_key_value_pairs", args).await?;
        Ok(Decode!(&bytes, Result6)?)
    }
}
